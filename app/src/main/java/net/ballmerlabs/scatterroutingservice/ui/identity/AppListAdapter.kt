package net.ballmerlabs.scatterroutingservice.ui.identity

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ballmerlabs.scatterbrainsdk.NamePackage
import net.ballmerlabs.scatterroutingservice.R
import net.ballmerlabs.scatterroutingservice.databinding.AppListItemBinding
import java.util.*

class AppListAdapter(private val context: Context, private val scope: CoroutineScope)
    : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    val items: TreeSet<NamePackage> = TreeSet()
    var onClickListener: suspend (id: NamePackage, isChecked: Boolean) -> Boolean = { _, _ ->  true}
    var isEnabledListener: suspend (id: NamePackage) -> Boolean = {_ -> true}

    inner class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {
        private val binding = AppListItemBinding.bind(view)

        var id: NamePackage? = null

        init {
            binding.grantCheckbox.setOnCheckedChangeListener { button, b ->
                val pack = id
                if (pack != null) {
                    scope.launch { button.isChecked = onClickListener(pack, b) }
                } else {
                    button.isChecked = false
                }
            }
        }

        var icon: Drawable
            get() = binding.appIcon.drawable
            set(value) {
                binding.appIcon.setImageDrawable(value)
            }

        var name: CharSequence
            get() = binding.appName.text
            set(value) {
                binding.appName.text = value
            }

        var checked: Boolean
            get() = binding.grantCheckbox.isChecked
            set(value) {
                binding.grantCheckbox.isChecked = value
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.app_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.toTypedArray()[position]
        holder.id = item
        holder.icon = item.icon?: AppCompatResources.getDrawable(context, R.drawable.ic_baseline_android_24)!!
        holder.name = item.name
        scope.launch {
            holder.checked = isEnabledListener(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}