package me.saket.dank.ui.submission.events

import android.content.Context
import me.saket.dank.ui.UiEvent
import me.saket.dank.ui.submission.comment.TextSelectionDialogActivity

data class CommentLongClicked(private val text: CharSequence) : UiEvent {

  fun openTextSelectionDialog(context: Context) {
    val intent = TextSelectionDialogActivity.intent(context, text)
    context.startActivity(intent)
  }
}
