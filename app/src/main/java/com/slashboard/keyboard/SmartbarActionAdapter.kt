package com.slashboard.keyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.slashboard.keyboard.data.model.SmartbarAction
import java.util.Collections

class SmartbarActionAdapter(
    var actions: MutableList<SmartbarAction>,
    private val isPool: Boolean,
    private val onItemClick: (SmartbarAction, Int) -> Unit
) : RecyclerView.Adapter<SmartbarActionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_icon)
        val title: TextView = view.findViewById(R.id.tv_title)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < actions.size) {
                    onItemClick(actions[pos], pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_smartbar_action, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val action = actions[position]
        holder.title.text = action.title
        
        val context = holder.itemView.context
        val resId = context.resources.getIdentifier(action.iconResName, "drawable", context.packageName)
        if (resId != 0) {
            holder.icon.setImageResource(resId)
        }
        
        holder.icon.alpha = if (isPool) 0.5f else 1.0f
    }

    override fun getItemCount() = actions.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(actions, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(actions, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }
}
