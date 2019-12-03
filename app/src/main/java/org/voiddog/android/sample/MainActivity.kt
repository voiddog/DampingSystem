package org.voiddog.android.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val adapter = MainMenuAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rec_list.adapter = adapter
        rec_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        adapter.menuDataList.add(MenuData("跳转到 OverScrollAppBarLayout", View.OnClickListener {
            startActivity(Intent(this, OverScrollAppBarActivity::class.java))
        }))
        adapter.menuDataList.add(MenuData("跳转到 SampleOverScrollView", View.OnClickListener {
            startActivity(Intent(this, DampScrollActivity::class.java))
        }))
        adapter.menuDataList.add(MenuData("跳转到 NestedDampLayout", View.OnClickListener {
            startActivity(Intent(this, NestedDampLayoutActivity::class.java))
        }))
        adapter.menuDataList.add(MenuData("跳转到 SwipeRefreshLayout", View.OnClickListener {
            startActivity(Intent(this, SwipeRefreshActivity::class.java))
        }))
    }
}
