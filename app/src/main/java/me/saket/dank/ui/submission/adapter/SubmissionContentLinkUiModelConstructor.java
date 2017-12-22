package me.saket.dank.ui.submission.adapter;

import static java.util.Arrays.asList;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.google.auto.value.AutoValue;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import me.saket.dank.R;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.links.ExternalLink;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.LinkMetadata;
import me.saket.dank.data.links.RedditLink;
import me.saket.dank.utils.Colors;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.utils.Urls;
import timber.log.Timber;

/**
 * Loads thumbnail, favicon and generates tint for a {@link Link}.
 */
public class SubmissionContentLinkUiModelConstructor {

  private static final boolean PROGRESS_VISIBLE = true;
  private static final boolean PROGRESS_HIDDEN = false;
  private final LinkMetadataRepository linkMetadataRepository;

  @Inject
  public SubmissionContentLinkUiModelConstructor(LinkMetadataRepository linkMetadataRepository) {
    this.linkMetadataRepository = linkMetadataRepository;
  }

  /**
   * Emits multiple times:
   * - Initially, with unparsed values.
   * - When title is loaded.
   * - When favicon is loaded.
   * - When thumbnail is loaded.
   * - When tint is generated.
   */
  public Observable<SubmissionContentLinkUiModel> streamLoad(Context context, Link link, ImageWithMultipleVariants redditSuppliedThumbnails) {
    int windowBackgroundColor = ContextCompat.getColor(context, R.color.window_background);

    if (link.isExternal()) {
      return externalLink(context, (ExternalLink) link, windowBackgroundColor, redditSuppliedThumbnails)
          //.doOnNext(model -> Timber.i("LinkUiModel: [title=%s, icon=%s, thumbnail=%s]", model.title(), model.icon(), model.thumbnail()))
          ;

    } else {
      Timber.i("link: %s ", link.unparsedUrl());
      // TODO.
      return Observable.just(SubmissionContentLinkUiModel.builder()
          .title(link.unparsedUrl())
          .titleMaxLines(2)
          .titleTextColorRes(R.color.submission_link_title)
          .byline(Urls.parseDomainName(link.unparsedUrl()))
          .bylineTextColorRes(R.color.submission_link_byline)
          .icon(Optional.empty())
          .thumbnail(Optional.empty())
          .progressVisible(false)
          .backgroundTintColor(Optional.empty())
          .build());
    }
  }

  // TODO: Use Reddit supplied thumbnail.
  public Observable<SubmissionContentLinkUiModel> externalLink(
      Context context,
      ExternalLink link,
      int windowBackgroundColor,
      ImageWithMultipleVariants redditSuppliedThumbnails)
  {
    Single<LinkMetadata> linkMetadataSingle = linkMetadataRepository.unfurl(link).delay(2, TimeUnit.SECONDS);

    // Title.
    Observable<String> sharedTitleStream = linkMetadataSingle
        .toObservable()
        .map(metadata -> metadata.title())
        .startWith(link.unparsedUrl())
        .share();

    // Favicon.
    //noinspection ConstantConditions
    Observable<Optional<Bitmap>> sharedFaviconStream = linkMetadataSingle
        .flatMapObservable(metadata -> metadata.hasFavicon() ? Observable.just(metadata.faviconUrl()) : Observable.empty())
        .flatMap(faviconUrl -> loadImage(context, faviconUrl))
        .map(favicon -> Optional.of(favicon))
        .startWith(Optional.empty())
        .share();

    // Thumbnail.
    Observable<Optional<Bitmap>> sharedThumbnailStream = Observable.just(redditSuppliedThumbnails.isNonEmpty())
        .flatMap(hasRedditSuppliedImages -> {
          if (hasRedditSuppliedImages) {
            int thumbnailWidth = SubmissionCommentsHeader.getWidthForAlbumContentLinkThumbnail(context);
            return Observable.just(redditSuppliedThumbnails.findNearestFor(thumbnailWidth));
          } else {
            //noinspection ConstantConditions
            return linkMetadataSingle.flatMapObservable(metadata -> metadata.hasImage()
                ? Observable.just(metadata.imageUrl())
                : Observable.empty());
          }
        })
        .flatMap(imageUrl -> loadImage(context, imageUrl))
        .map(image -> Optional.of(image))
        .startWith(Optional.empty())
        .share();

    // Progress.
    Observable<Boolean> progressVisibleStream = Completable
        .mergeDelayError(asList(sharedTitleStream.ignoreElements(), sharedFaviconStream.ignoreElements(), sharedThumbnailStream.ignoreElements()))
        .andThen(Observable.just(PROGRESS_HIDDEN))
        .onErrorReturnItem(PROGRESS_HIDDEN)
        .startWith(PROGRESS_VISIBLE);

    TintDetails defaultTintDetails = TintDetails.create(Optional.empty(), R.color.submission_link_title, R.color.submission_link_byline);
    boolean isGooglePlayThumbnail = UrlParser.isGooglePlayUrl(Uri.parse(link.unparsedUrl()));

    Observable<TintDetails> tintDetailsStream = Observable.concat(sharedThumbnailStream, sharedFaviconStream)
        .filter(imageOptional -> imageOptional.isPresent())
        .take(1)
        .map(imageOptional -> imageOptional.get())
        .flatMapSingle(image -> generateTint(image, isGooglePlayThumbnail, windowBackgroundColor))
        .startWith(defaultTintDetails);

    return Observable.combineLatest(sharedTitleStream, sharedFaviconStream, sharedThumbnailStream, tintDetailsStream, progressVisibleStream,
        (title, optionalFavicon, optionalThumbnail, tintDetails, progressVisible) ->
            SubmissionContentLinkUiModel.builder()
                .title(title)
                .titleMaxLines(2)
                .titleTextColorRes(tintDetails.titleTextColorRes())
                .byline(Urls.parseDomainName(link.unparsedUrl()))
                .bylineTextColorRes(tintDetails.bylineTextColorRes())
                .icon(optionalFavicon)
                .thumbnail(optionalThumbnail)
                .progressVisible(progressVisible)
                .backgroundTintColor(tintDetails.backgroundTint())
                .build());
  }

  private Observable<Bitmap> loadImage(Context context, String faviconUrl) {
    return Observable
        .<Bitmap>create(emitter -> {
          FutureTarget<Bitmap> futureTarget = Glide.with(context)
              .asBitmap()
              .load(faviconUrl)
              .submit();
          emitter.onNext(futureTarget.get());
          emitter.onComplete();
          emitter.setCancellable(() -> Glide.with(context).clear(futureTarget));
        })
        .onErrorResumeNext(error -> {
          Timber.e(error.getMessage());
          return Observable.empty();
        });
  }

  private Single<TintDetails> generateTint(Bitmap bitmap, boolean isGooglePlayThumbnail, int windowBackgroundColor) {
    return Single.fromCallable(() -> Palette.from(bitmap)
        .maximumColorCount(Integer.MAX_VALUE)    // Don't understand why, but this changes the darkness of the colors.
        .generate())
        .map(palette -> {
          int tint = -1;
          if (isGooglePlayThumbnail) {
            tint = palette.getLightVibrantColor(-1);
          }
          if (tint == -1) {
            // Mix the color with the window's background color to neutralize any possibly strong
            // colors (e.g., strong blue, pink, etc.)
            tint = Colors.mix(windowBackgroundColor, palette.getVibrantColor(-1));
          }
          if (tint == -1) {
            tint = Colors.mix(windowBackgroundColor, palette.getMutedColor(-1));
          }
          return tint != -1
              ? Optional.of(tint)
              : Optional.<Integer>empty();
        })
        .map(tintColorOptional -> {
          // Inverse title and byline colors when the background tint is light.
          boolean isLightBackgroundTint = tintColorOptional.isPresent() && Colors.isLight(tintColorOptional.get());
          int titleColorRes = isLightBackgroundTint
              ? R.color.submission_link_title_light_background
              : R.color.submission_link_title;
          int bylineColorRes = isLightBackgroundTint
              ? R.color.submission_link_byline_light_background
              : R.color.submission_link_byline;
          return TintDetails.create(tintColorOptional, titleColorRes, bylineColorRes);
        });
  }

  @AutoValue
  abstract static class TintDetails {

    public abstract Optional<Integer> backgroundTint();

    @ColorRes
    public abstract int titleTextColorRes();

    @ColorRes
    public abstract int bylineTextColorRes();

    public static TintDetails create(Optional<Integer> backgroundTint, @ColorRes int titleTextColorRes, @ColorRes int bylineTextColorRes) {
      return new AutoValue_SubmissionContentLinkUiModelConstructor_TintDetails(backgroundTint, titleTextColorRes, bylineTextColorRes);
    }
  }

  public void loadReddit(RedditLink link) {

  }

  public void imgurAlbum(ImgurAlbumLink link) {

  }
}
