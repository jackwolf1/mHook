package com.example.myactivityhook

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myactivityhook.hook.HookHelper
import com.example.myactivityhook.hook.HookHelper.hookHandler
import com.example.myactivityhook.hook.HookHelper.hookIActivityManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setListener()

    }

    private fun setListener() {
        button.setOnClickListener {
            hookIActivityManager()
            hookHandler()
            val intent = Intent(this, TargetActivity::class.java)
            startActivity(intent)
        }
        button2.setOnClickListener {
            HookHelper.hookInstrumentation(this)
            val intent = Intent(this, StubActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        button3.setOnClickListener {
            HookHelper.hookActivityThreadInstrumentation()
            val intent = Intent(this, StubActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        button4.setOnClickListener {
            HookHelper.hookAMS()
            val intent = Intent(this, StubActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}
