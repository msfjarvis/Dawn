package me.saket.dank.ui.submission.comment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import me.saket.dank.R
import me.saket.dank.ui.DankActivity

class TextSelectionDialogActivity : DankActivity() {

  @BindView(R.id.textselectiondialog_text)
  lateinit var selectableTextView: TextView

  @BindView(R.id.textselectiondialog_root)
  lateinit var rootGroup: ViewGroup

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_dialog_text_selection)
    ButterKnife.bind(this)

    window.setBackgroundDrawableResource(R.color.dialog_like_activity_window_background)

    overridePendingTransition(R.anim.dialog_fade_in, 0)

    selectableTextView.text = intent.getCharSequenceExtra(EXTRA_TEXT)
    rootGroup.setOnClickListener {
      finish()
    }
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, R.anim.dialog_fade_out)
  }

  companion object {
    private const val EXTRA_TEXT = "TextSelectionDialogActivity.text"

    @JvmStatic
    fun intent(context: Context, text: CharSequence) =
      Intent(context, TextSelectionDialogActivity::class.java).apply {
        putExtra(EXTRA_TEXT, text)
      }
  }
}
