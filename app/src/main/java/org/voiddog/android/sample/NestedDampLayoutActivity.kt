package org.voiddog.android.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_nested_damp_layout.*

class NestedDampLayoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nested_damp_layout)
        val adapter = TestAdapter()
        for (i in 1..20) {
            adapter.contentList.add("我是${i}号")
        }
        rec_list.layoutManager = LinearLayoutManager(this)
        rec_list.adapter = adapter
    }
}
