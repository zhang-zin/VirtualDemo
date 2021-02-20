package com.zj.virtualdemo.adapters

import android.content.pm.LauncherActivityInfo
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.zj.virtualdemo.R
import com.zj.virtualdemo.databinding.AllAppItemBinding
import com.zj.virtualdemo.databinding.AppListItemBinding
import com.zj.virtualdemo.models.AppInfo

class CloneAppsAdapter(
    private val apps: List<LauncherActivityInfo>,
    private val listener: ((item: LauncherActivityInfo, view: View) -> Unit)? = null
) : RecyclerView.Adapter<CloneAppsAdapter.AllAppItemHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllAppItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            DataBindingUtil.inflate<AllAppItemBinding>(
                inflater,
                R.layout.all_app_item,
                parent,
                false
            )
        return AllAppItemHolder(binding)
    }


    override fun onBindViewHolder(holder: AllAppItemHolder, position: Int) {
        holder.bind(apps[position], listener)
        holder.binding.ivAppIcon.setImageDrawable(apps[position].getIcon(DisplayMetrics.DENSITY_DEFAULT))
        holder.binding.tvAppLabel.text = apps[position].label
    }

    override fun getItemCount() = apps.size

    class AllAppItemHolder(val binding: AllAppItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            appInfo: LauncherActivityInfo,
            listener: ((item: LauncherActivityInfo, view: View) -> Unit)?
        ) {
            binding.executePendingBindings()
            binding.setClickListener {
                listener?.invoke(appInfo, binding.root)
            }
        }
    }


}