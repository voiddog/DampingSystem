package org.voiddog.android.sample

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

/**
 * ┏┛ ┻━━━━━┛ ┻┓
 * ┃　　　　　　 ┃
 * ┃　　　━　　　┃
 * ┃　┳┛　  ┗┳　┃
 * ┃　　　　　　 ┃
 * ┃　　　┻　　　┃
 * ┃　　　　　　 ┃
 * ┗━┓　　　┏━━━┛
 * * ┃　　　┃   神兽保佑
 * * ┃　　　┃   代码无BUG！
 * * ┃　　　┗━━━━━━━━━┓
 * * ┃　　　　　　　    ┣┓
 * * ┃　　　　         ┏┛
 * * ┗━┓ ┓ ┏━━━┳ ┓ ┏━┛
 * * * ┃ ┫ ┫   ┃ ┫ ┫
 * * * ┗━┻━┛   ┗━┻━┛
 * @author qigengxin
 * @since 2018-05-03 10:12
 */

data class MenuData(val content:String, val listener:View.OnClickListener)

class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.rec_item_main_menu, parent, false)) {
    val btnJump = itemView.findViewById<Button>(R.id.btn_jump)

    private var bindData: MenuData? = null

    init {
        btnJump.setOnClickListener {
            bindData?.listener?.onClick(it)
        }
    }

    fun bind(data: MenuData) {
        bindData = data
        btnJump.text = data.content
    }
}

class MainMenuAadpter : RecyclerView.Adapter<VH>() {

    val menuDataList = ArrayList<MenuData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(parent)

    override fun getItemCount(): Int = menuDataList.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(menuDataList[position])
    }
}