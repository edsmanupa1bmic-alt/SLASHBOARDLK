package com.slashboard.keyboard

import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.slashboard.keyboard.data.model.SmartbarAction

class SmartbarSettingsActivity : AppCompatActivity() {

    private lateinit var activeAdapter: SmartbarActionAdapter
    private lateinit var disabledAdapter: SmartbarActionAdapter
    
    private val activeActions = mutableListOf<SmartbarAction>()
    private val disabledActions = mutableListOf<SmartbarAction>()
    private val prefs by lazy { SlashboardApp.instance.preferencesRepository }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smartbar_customizer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val settings = prefs.settingsFlow.value
        
        if (settings.smartbarActiveActions.isNotEmpty()) {
            settings.smartbarActiveActions.forEach { id ->
                SmartbarAction.fromId(id)?.let { activeActions.add(it) }
            }
        } else {
            activeActions.addAll(SmartbarAction.DEFAULT_ACTIVE)
        }
        
        if (settings.smartbarDisabledActions.isNotEmpty()) {
            settings.smartbarDisabledActions.forEach { id ->
                SmartbarAction.fromId(id)?.let { disabledActions.add(it) }
            }
        } else {
            disabledActions.addAll(SmartbarAction.DEFAULT_DISABLED)
        }

        activeAdapter = SmartbarActionAdapter(activeActions, false) { action, pos ->
            // Move to disabled
            if (pos in 0 until activeActions.size) {
                activeActions.removeAt(pos)
                activeAdapter.notifyItemRemoved(pos)
                disabledActions.add(action)
                disabledAdapter.notifyItemInserted(disabledActions.size - 1)
                savePreferences()
            }
        }

        disabledAdapter = SmartbarActionAdapter(disabledActions, true) { action, pos ->
            // Move to active
            if (pos in 0 until disabledActions.size) {
                disabledActions.removeAt(pos)
                disabledAdapter.notifyItemRemoved(pos)
                activeActions.add(action)
                activeAdapter.notifyItemInserted(activeActions.size - 1)
                savePreferences()
            }
        }

        val rvActive = findViewById<RecyclerView>(R.id.rv_active_actions)
        rvActive.layoutManager = GridLayoutManager(this, 5)
        rvActive.adapter = activeAdapter
        
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(activeAdapter))
        itemTouchHelper.attachToRecyclerView(rvActive)

        val rvDisabled = findViewById<RecyclerView>(R.id.rv_disabled_actions)
        rvDisabled.layoutManager = GridLayoutManager(this, 5)
        rvDisabled.adapter = disabledAdapter

        findViewById<Button>(R.id.btn_reset_default).setOnClickListener {
            activeActions.clear()
            activeActions.addAll(SmartbarAction.DEFAULT_ACTIVE)
            disabledActions.clear()
            disabledActions.addAll(SmartbarAction.DEFAULT_DISABLED)
            activeAdapter.notifyDataSetChanged()
            disabledAdapter.notifyDataSetChanged()
            savePreferences()
        }
    }

    override fun onPause() {
        super.onPause()
        savePreferences()
    }

    private fun savePreferences() {
        val activeIds = activeActions.map { it.id }
        val disabledIds = disabledActions.map { it.id }
        prefs.updateSmartbarActions(activeIds, disabledIds)
        
        try {
            getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
                .edit()
                .putString("smartbar_active_actions", activeIds.joinToString(","))
                .putString("smartbar_disabled_actions", disabledIds.joinToString(","))
                .apply()
        } catch (_: Throwable) {}
    }
}
