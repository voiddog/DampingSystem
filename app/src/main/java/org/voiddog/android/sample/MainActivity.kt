package org.voiddog.android.sample

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_damp_scroll_view.setOnClickListener {
            startActivity(Intent(this, DampScrollActivity::class.java))
        }
        btn_over_scroll.setOnClickListener {
            startActivity(Intent(this, OverScrollAppBarActivity::class.java))
        }
    }
}
