package me.saket.dank.ui.preferences.adapter

import android.content.Context
import com.f2prateek.rx.preferences2.Preference
import me.saket.dank.BuildConfig
import me.saket.dank.R
import me.saket.dank.ui.preferences.MultiOptionPreferencePopup
import me.saket.dank.ui.preferences.TypefaceResource
import me.saket.dank.ui.subreddit.SubmissionSwipeAction
import me.saket.dank.ui.subreddit.uimodels.SubredditSubmissionImageStyle
import javax.inject.Inject
import javax.inject.Named

class LookAndFeelPreferencesConstructor @Inject constructor(
  private val typefacePref: Preference<TypefaceResource>,
  @Named("comment_count_in_submission_list_byline") private val showCommentCountInByline: Preference<Boolean>,
  @Named("show_submission_thumbnails_on_left") private val showSubmissionThumbnailsOnLeft: Preference<Boolean>,
  @Named("show_colored_comments_tree") private val showColoredCommentsTree: Preference<Boolean>,
  @Named("submission_start_swipe_actions") private val submissionStartSwipeActions: Preference<List<SubmissionSwipeAction>>,
  @Named("submission_end_swipe_actions") private val submissionEndSwipeActions: Preference<List<SubmissionSwipeAction>>,
  @Named("subreddit_submission_image_style") private val subredditSubmissionImageStyle: Preference<SubredditSubmissionImageStyle>
) : UserPreferencesConstructor.ChildConstructor {

  override fun construct(c: Context): List<UserPreferencesScreenUiModel> {
    val uiModels = mutableListOf<UserPreferencesScreenUiModel>()

    if (true) {
      val typefaceResource = typefacePref.get()

      uiModels.add(UserPreferenceSectionHeader.UiModel.create("Text"))
      uiModels.add(UserPreferenceButton.UiModel.create(
        "Typeface",
        typefaceResource.name()
      ) { clickHandler, event ->
        val builder = MultiOptionPreferencePopup.builder(typefacePref)
        add(builder, TypefaceResource.create("Roboto", "roboto_regular.ttf"))
        add(builder, TypefaceResource.create("Roboto condensed", "RobotoCondensed-Regular.ttf"))

        add(builder, TypefaceResource.create("Bifocals", "bifocals.otf"))

        add(builder, TypefaceResource.create("Transcript regular", "TranscriptTrial-Regular.otf"))

        add(builder, TypefaceResource.create("Basis", "basis-grotesque-trial-regular.otf"))
        add(builder, TypefaceResource.create("Basis medium", "basis-grotesque-trial-medium.otf"))

        add(builder, TypefaceResource.create("Relative book", "relativetrial-book.otf"))
        add(builder, TypefaceResource.create("Relative faux", "relativetrial-faux.otf"))
        add(builder, TypefaceResource.create("Relative medium", "relativetrial-medium.otf"))
        add(builder, TypefaceResource.create("Relative mono 12", "relativetrial-mono12pitch.otf"))

        add(builder, TypefaceResource.create("LaFabrique", "LaFabriqueTrial-Regular.otf"))
        add(builder, TypefaceResource.create("LaFabrique SemiBold", "LaFabriqueTrial-SemiBold.otf"))

        add(builder, TypefaceResource.create("Reader", "readertrial-regular.otf"))
        add(builder, TypefaceResource.create("Reader medium", "readertrial-medium.otf"))

        add(builder, TypefaceResource.create("Inter", "Inter-UI-Regular.otf"))
        add(builder, TypefaceResource.create("Inter medium", "Inter-UI-Medium.otf"))

        add(builder, TypefaceResource.create("Visuelt", "visuelttrial-regular.otf"))

        clickHandler.show(builder, event.itemViewHolder())
      })
    }

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_subreddit)))

    uiModels.add(UserPreferenceButton.UiModel.create(
      c.getString(R.string.userprefs_submission_image_style),
      c.getString(subredditSubmissionImageStyle.get().userVisibleName)
    ) { clickHandler, event ->
      val builder = MultiOptionPreferencePopup.builder(subredditSubmissionImageStyle)
      builder.addOption(SubredditSubmissionImageStyle.NONE, R.string.image_style_disabled, R.drawable.ic_block_20dp)
      builder.addOption(SubredditSubmissionImageStyle.THUMBNAIL, R.string.image_style_thumbnail, R.drawable.ic_image_style_thumbnail_20dp)
      builder.addOption(SubredditSubmissionImageStyle.LARGE, R.string.image_style_large, R.drawable.ic_image_style_large_20dp)
      builder.addOption(SubredditSubmissionImageStyle.FULL_HEIGHT, R.string.image_style_full_height, R.drawable.ic_image_style_full_height_20dp)
      clickHandler.show(builder, event.itemViewHolder())
    })

    uiModels.add(
      UserPreferenceSwitch.UiModel(
        if (subredditSubmissionImageStyle.get() == SubredditSubmissionImageStyle.THUMBNAIL)
          c.getString(R.string.userprefs_show_submission_thumbnails_on_left)
        else
          c.getString(R.string.userprefs_show_submission_type_indicator_on_left),
        if (showSubmissionThumbnailsOnLeft.get())
          c.getString(R.string.userprefs_show_submission_thumbnails_on_left_summary_on)
        else
          c.getString(R.string.userprefs_show_submission_thumbnails_on_left_summary_off),
        showSubmissionThumbnailsOnLeft.get(),
        showSubmissionThumbnailsOnLeft,
        subredditSubmissionImageStyle.get() != SubredditSubmissionImageStyle.NONE
      )
    )

    uiModels.add(
      UserPreferenceSwitch.UiModel(
        c.getString(R.string.userprefs_item_byline_comment_count),
        if (showCommentCountInByline.get())
          c.getString(R.string.userprefs_item_byline_comment_count_summary_on)
        else
          c.getString(R.string.userprefs_item_byline_comment_count_summary_off),
        showCommentCountInByline.get(),
        showCommentCountInByline
      )
    )

    uiModels.add(
      UserPreferenceSwitch.UiModel(
        c.getString(R.string.userprefs_show_colored_comments_tree),
        if (showColoredCommentsTree.get())
          c.getString(R.string.userprefs_show_colored_comments_tree_on)
        else
          c.getString(R.string.userprefs_show_colored_comments_tree_off),
        showColoredCommentsTree.get(),
        showColoredCommentsTree
      )
    )

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_gestures)))

    uiModels.add(UserPreferenceButton.UiModel.create(
      c.getString(R.string.userprefs_customize_submission_gestures),
      buildSubmissionGesturesSummary(c)
    ) { clickHandler, event ->
      clickHandler.expandNestedPage(
        R.layout.view_user_preferences_submission_gestures,
        event.itemViewHolder()
      )
    })

    uiModels.add(UserPreferenceButton.UiModel.create(
      c.getString(R.string.userprefs_customize_comment_gestures),
      "Reply and Options\nUpvote and Downvote"
    ) { clickHandler, event ->
      clickHandler.expandNestedPage(
        R.layout.view_user_preferences_comment_gestures,
        event.itemViewHolder()
      )
    })

    return uiModels
  }

  private fun add(builder: MultiOptionPreferencePopup.Builder<TypefaceResource>, resource: TypefaceResource) {
    builder.addOption(resource, resource.name(), R.drawable.ic_text_fields_20dp)
  }

  private fun buildSubmissionGesturesSummary(c: Context): String {
    fun buildSummaryForSingleSide(actions: List<SubmissionSwipeAction>): String {
      if (actions.isEmpty()) {
        return c.getString(R.string.userprefs_gestures_summary_disabled)
      }

      val separator = ", "
      val joinedNames = actions.joinToString(separator = separator) { c.getString(it.displayNameRes) }
      val indexOfLastSeparator = joinedNames.lastIndexOf(separator)
      if (indexOfLastSeparator >= 0) {
        return joinedNames.replaceRange(
          indexOfLastSeparator,
          indexOfLastSeparator + separator.length,
          " ${c.getString(R.string.userprefs_gestures_summary_last_separator)} "
        )
      }
      return joinedNames
    }

    val startActionsText = buildSummaryForSingleSide(submissionStartSwipeActions.get())
    val endActionsText = buildSummaryForSingleSide(submissionEndSwipeActions.get())
    return "$startActionsText\n$endActionsText"
  }
}
