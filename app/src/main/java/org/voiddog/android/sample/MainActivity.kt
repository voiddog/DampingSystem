package org.voiddog.android.sample

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val adapter = MainMenuAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rec_list.adapter = adapter
        rec_list.layoutManager = LinearLayoutManager(this)
        adapter.menuDataList.add(MenuData("跳转到 OverScrollAppBarLayout", View.OnClickListener {
            startActivity(Intent(this, OverScrollAppBarActivity::class.java))
        }))
        adapter.menuDataList.add(MenuData("跳转到 SampleOverScrollView", View.OnClickListener {
            startActivity(Intent(this, DampScrollActivity::class.java))
        }))
    }
}
