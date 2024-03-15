package com.example.smsboomber.uitilts

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.smsboomber.R
import com.example.smsboomber.databinding.HistoryItemLayoutBinding
import com.example.smsboomber.model.Message


class CatecoriesAdapter(
    var items: ArrayList<Message>,
    var listener: ItemSetOnClickListener) :
    RecyclerView.Adapter<CatecoriesAdapter.Holder>() {

    interface ItemSetOnClickListener {
        fun onClick(data: Message)
    }

    inner class Holder(var view: HistoryItemLayoutBinding) : RecyclerView.ViewHolder(view.root) {

        fun bind(data: Message) {

            view.apply {

                this.tvIndex.text=data.messageIndex
                this.tvPhone.text=data.number
                this.tvSms.text=data.message


            }

        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {

        val binding =
            HistoryItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return Holder(
            binding
        )


    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]

        holder.bind(item)
        holder.itemView.setOnClickListener {
            listener.onClick(item)
        }

    }

    override fun getItemCount(): Int = items.count()

}