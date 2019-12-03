package org.voiddog.android.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_swipe_refresh.*

class SwipeRefreshActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swipe_refresh)
        val adapter = TestAdapter()
        for (i in 1..50) {
            adapter.contentList.add("测试内容${i}")
        }
        rec_list.adapter = adapter
        rec_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        swipe_refresh.setOnRefreshListener {
            rec_list.postDelayed({
                val adapter = rec_list.adapter as TestAdapter
                for (i in 1..50) {
                    adapter.contentList.add("刷新内容${i}")
                }
                adapter.notifyDataSetChanged()
            }, 3000)
        }
    }
}
