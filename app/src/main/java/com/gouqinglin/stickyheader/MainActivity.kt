package com.gouqinglin.stickyheader

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gouqinglin.stickyheader.lib.OnStickyStateChangedListener
import com.gouqinglin.stickyheader.lib.StickyLinearLayout

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 处理系统窗口边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewTop)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            v.layoutParams.height = systemBars.top
            insets
        }

        // 吸附监听
        val sticky = findViewById<StickyLinearLayout>(R.id.sticky)
        sticky.setOnStickyStateChangedListener(object : OnStickyStateChangedListener {
            override fun onViewPinned(view: View, index: Int) {
                Log.d(TAG, "onViewPinned: view=${view.id}, index=$index")
            }

            override fun onViewUnpinned(view: View, index: Int) {
                Log.d(TAG, "onViewUnpinned: view=${view.id}, index=$index")
            }

            override fun onPinnedViewOffsetChanged(view: View, index: Int, offset: Int) {
                super.onPinnedViewOffsetChanged(view, index, offset)
                Log.d(TAG, "onPinnedViewOffsetChanged: view=${view.id}, index=$index, offset=$offset")
            }

            override fun onPinnedViewsChanged(pinnedViews: List<View>) {
                super.onPinnedViewsChanged(pinnedViews)
                Log.d(TAG, "onPinnedViewsChanged: pinnedViews=${pinnedViews.map { it.id }}")
            }

            override fun onPinnedHeightChanged(totalPinnedHeight: Int) {
                super.onPinnedHeightChanged(totalPinnedHeight)
                Log.d(TAG, "onPinnedHeightChanged: totalPinnedHeight=$totalPinnedHeight")
            }
        })

        // 设置RecyclerView
        val rv = findViewById<RecyclerView>(R.id.recycler)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = DemoAdapter(List(50) { index -> "Item #${index + 1}" })
    }
}