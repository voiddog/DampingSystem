package org.voiddog.android.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_nested_damp_layout.*
import org.voiddog.android.damp.view.NestedDampLayout

class NestedDampLayoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nested_damp_layout)
        val adapter = TestAdapter()
        for (i in 1..50) {
            adapter.contentList.add("我是${i}号")
        }
        rec_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rec_list.adapter = adapter
        damp_layout.setDampFlag(NestedDampLayout.DAMP_FLAG_START)
    }
}
