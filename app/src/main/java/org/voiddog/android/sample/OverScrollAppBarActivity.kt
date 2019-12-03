package org.voiddog.android.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_over_scroll_app_bar.*

class OverScrollAppBarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_over_scroll_app_bar)
        rec_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rec_list.adapter = TestAdapter()
        for (i in 1..100) {
            (rec_list.adapter as TestAdapter).contentList.add("测试 $i")
        }
        btn_reset.setOnClickListener {
            over_scroll_bar.behavior?.forceOffset(0f)
        }
    }
}
