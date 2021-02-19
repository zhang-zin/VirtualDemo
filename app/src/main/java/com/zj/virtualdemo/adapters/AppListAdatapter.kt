package com.zj.virtualdemo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.zj.virtualdemo.R
import com.zj.virtualdemo.databinding.AppListItemBinding
import com.zj.virtualdemo.models.AppInfo

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val listener: ((item: AppInfo) -> Unit)? = null
) :
    RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            DataBindingUtil.inflate<AppListItemBinding>(
                inflater,
                R.layout.app_list_item,
                parent,
                false
            )
        return AppListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        holder.bind(apps[position], listener)
    }

    override fun getItemCount() = apps.size

    class AppListViewHolder(private val binding: AppListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appInfo: AppInfo, listener: ((item: AppInfo) -> Unit)?) {
            binding.appInfo = appInfo
            binding.executePendingBindings()
            binding.setClickListener {
                listener?.invoke(appInfo)
            }
        }
    }
}