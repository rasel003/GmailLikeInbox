package com.rasel.myapplication

import android.text.method.TextKeyListener.clear
import java.nio.file.Files.delete
import androidx.core.content.ContextCompat
import android.graphics.Typeface
import android.R.id.message
import android.content.Context
import androidx.core.view.ViewCompat.setRotationY
import androidx.core.view.ViewCompat.getRotationY
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.Glide
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import android.view.View.OnLongClickListener
import androidx.core.view.ViewCompat.setActivated
import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.SparseBooleanArray
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class MessagesAdapter(
    private val mContext: Context,
    private val messages: MutableList<Message>,
    private val listener: MessageAdapterListener
) : RecyclerView.Adapter<MessagesAdapter.MyViewHolder>() {
    private val selectedItems: SparseBooleanArray

    // array used to perform multiple animation at once
    private val animationItemsIndex: SparseBooleanArray
    private var reverseAllAnimations = false

    val selectedItemCount: Int
        get() = selectedItems.size()

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnLongClickListener {
        var from: TextView
        var subject: TextView
        var message: TextView
        var iconText: TextView
        var timestamp: TextView
        var iconImp: ImageView
        var imgProfile: ImageView
        var messageContainer: LinearLayout
        var iconContainer: RelativeLayout
        var iconBack: RelativeLayout
        var iconFront: RelativeLayout

        init {
            from = view.findViewById(R.id.from)
            subject = view.findViewById(R.id.txt_primary)
            message = view.findViewById(R.id.txt_secondary)
            iconText = view.findViewById(R.id.icon_text)
            timestamp = view.findViewById(R.id.timestamp)
            iconBack = view.findViewById(R.id.icon_back)
            iconFront = view.findViewById(R.id.icon_front)
            iconImp = view.findViewById(R.id.icon_star) as ImageView
            imgProfile = view.findViewById(R.id.icon_profile) as ImageView
            messageContainer = view.findViewById(R.id.message_container)
            iconContainer = view.findViewById(R.id.icon_container)
            view.setOnLongClickListener(this)
        }

        override fun onLongClick(view: View): Boolean {
            listener.onRowLongClicked(adapterPosition)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }
    }


    init {
        selectedItems = SparseBooleanArray()
        animationItemsIndex = SparseBooleanArray()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_list_row, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message = messages[position]

        // displaying text view data
        holder.from.text = message.from
        holder.subject.text = message.subject
        holder.message.text = message.message
        holder.timestamp.text = message.timestamp

        // displaying the first letter of From in icon text
        holder.iconText.text = message.from!!.substring(0, 1)

        // change the row state to activated
        holder.itemView.isActivated = selectedItems.get(position, false)

        // change the font style depending on message read status
        applyReadStatus(holder, message)

        // handle message star
        applyImportant(holder, message)

        // handle icon animation
        applyIconAnimation(holder, position)

        // display profile image
        applyProfilePicture(holder, message)

        // apply click events
        applyClickEvents(holder, position)
    }

    private fun applyClickEvents(holder: MyViewHolder, position: Int) {
        holder.iconContainer.setOnClickListener {
            listener.onIconClicked(position)
        }


        holder.iconImp.setOnClickListener {
                listener.onIconImportantClicked(position)
            }


        holder.messageContainer.setOnClickListener{
                listener.onMessageRowClicked(position)
            }


        holder.messageContainer.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(view: View): Boolean {
                listener.onRowLongClicked(position)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                return true
            }
        })
    }

    private fun applyProfilePicture(holder: MyViewHolder, message: Message) {
        if (!TextUtils.isEmpty(message.picture)) {
            Glide.with(mContext).load(message.picture)
                .thumbnail(0.5f)
                .transform(CircleTransform(mContext))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imgProfile)
            holder.imgProfile.setColorFilter(null)
            holder.iconText.visibility = View.GONE
        } else {
            holder.imgProfile.setImageResource(R.drawable.bg_circle)
            holder.imgProfile.setColorFilter(message.color)
            holder.iconText.visibility = View.VISIBLE
        }
    }

    private fun applyIconAnimation(holder: MyViewHolder, position: Int) {
        if (selectedItems.get(position, false)) {
            holder.iconFront.visibility = View.GONE
            resetIconYAxis(holder.iconBack)
            holder.iconBack.visibility = View.VISIBLE
            holder.iconBack.alpha = 1f
            if (currentSelectedIndex == position) {
                FlipAnimator.flipView(mContext, holder.iconBack, holder.iconFront, true)
                resetCurrentIndex()
            }
        } else {
            holder.iconBack.visibility = View.GONE
            resetIconYAxis(holder.iconFront)
            holder.iconFront.visibility = View.VISIBLE
            holder.iconFront.alpha = 1f
            if (reverseAllAnimations && animationItemsIndex.get(
                    position,
                    false
                ) || currentSelectedIndex == position
            ) {
                FlipAnimator.flipView(mContext, holder.iconBack, holder.iconFront, false)
                resetCurrentIndex()
            }
        }
    }


    // As the views will be reused, sometimes the icon appears as
    // flipped because older view is reused. Reset the Y-axis to 0
    private fun resetIconYAxis(view: View) {
        if (view.getRotationY() !== 0f) {
            view.setRotationY(0f)
        }
    }

    fun resetAnimationIndex() {
        reverseAllAnimations = false
        animationItemsIndex.clear()
    }

    override fun getItemId(position: Int): Long {
        return messages[position].id.toLong()
    }

    private fun applyImportant(holder: MyViewHolder, message: Message) {
        if (message.isImportant) {
            holder.iconImp.setImageDrawable(
                ContextCompat.getDrawable(
                    mContext,
                    R.drawable.ic_star_black_24dp
                )
            )
            holder.iconImp.setColorFilter(
                ContextCompat.getColor(
                    mContext,
                    R.color.icon_tint_selected
                )
            )
        } else {
            holder.iconImp.setImageDrawable(
                ContextCompat.getDrawable(
                    mContext,
                    R.drawable.ic_star_border_black_24dp
                )
            )
            holder.iconImp.setColorFilter(
                ContextCompat.getColor(
                    mContext,
                    R.color.icon_tint_normal
                )
            )
        }
    }

    private fun applyReadStatus(holder: MyViewHolder, message: Message) {
        if (message.isRead) {
            holder.from.setTypeface(null, Typeface.NORMAL)
            holder.subject.setTypeface(null, Typeface.NORMAL)
            holder.from.setTextColor(ContextCompat.getColor(mContext, R.color.subject))
            holder.subject.setTextColor(ContextCompat.getColor(mContext, R.color.message))
        } else {
            holder.from.setTypeface(null, Typeface.BOLD)
            holder.subject.setTypeface(null, Typeface.BOLD)
            holder.from.setTextColor(ContextCompat.getColor(mContext, R.color.from))
            holder.subject.setTextColor(ContextCompat.getColor(mContext, R.color.subject))
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun toggleSelection(pos: Int) {
        currentSelectedIndex = pos
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos)
            animationItemsIndex.delete(pos)
        } else {
            selectedItems.put(pos, true)
            animationItemsIndex.put(pos, true)
        }
        notifyItemChanged(pos)
    }

    fun clearSelections() {
        reverseAllAnimations = true
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<Int> {
        val items = ArrayList<Int>(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }

    fun removeData(position: Int) {
        messages.removeAt(position)
        resetCurrentIndex()
    }

    private fun resetCurrentIndex() {
        currentSelectedIndex = -1
    }

    interface MessageAdapterListener {
        fun onIconClicked(position: Int)

        fun onIconImportantClicked(position: Int)

        fun onMessageRowClicked(position: Int)

        fun onRowLongClicked(position: Int)
    }

    companion object {

        // index is used to animate only the selected row
        // dirty fix, find a better solution
        private var currentSelectedIndex = -1
    }
}