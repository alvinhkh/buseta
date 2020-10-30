package com.alvinhkh.buseta.appwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import com.alvinhkh.buseta.R
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import com.alvinhkh.buseta.follow.model.FollowGroup
import com.alvinhkh.buseta.follow.ui.FollowGroupViewModel


class FollowWidgetConfigurationActivity : AppCompatActivity() {
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var withHeaderCheckBox: CheckBox
    private lateinit var followGroupSpinner: AppCompatSpinner
    private lateinit var noOfRowsSpinner: AppCompatSpinner
    private lateinit var saveButton: Button

    private lateinit var preferences: WidgetPreferences
    var followGroupList = listOf<FollowGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        setTitle(R.string.widgets)

        withHeaderCheckBox = findViewById(R.id.with_header_checkbox)
        withHeaderCheckBox.isChecked = true
        saveButton = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            save()
        }

        followGroupSpinner = findViewById(R.id.follow_group_spinner)
        val followGroupAdapter = ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item)
        followGroupSpinner.adapter = followGroupAdapter

        val viewModel = ViewModelProvider(this).get(FollowGroupViewModel::class.java)
        viewModel.getAsLiveData().observe(this, { list ->
            followGroupAdapter.clear()
            followGroupList = list?.toList()?: emptyList()
            list.forEach { followGroup ->
                val name = when {
                    followGroup.id == FollowGroup.UNCATEGORISED -> getString(R.string.uncategorised)
                    followGroup.name.isNotEmpty() -> followGroup.name
                    else -> followGroup.id
                }
                followGroupAdapter.add(name)
            }
        })

        noOfRowsSpinner = findViewById(R.id.widget_item_rows_spinner)
        val noOfRowsAdapter = ArrayAdapter.createFromResource(this,
                R.array.widget_item_lines_entries, R.layout.support_simple_spinner_dropdown_item)
        noOfRowsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        noOfRowsSpinner.adapter = noOfRowsAdapter
        noOfRowsSpinner.setSelection(2)

        preferences = WidgetPreferences(this)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (widgetId <= AppWidgetManager.INVALID_APPWIDGET_ID) {
            //something went wrong
            finish()
        }
    }

    private fun save() {
        if (followGroupList.isEmpty() || followGroupList[followGroupSpinner.selectedItemPosition].id.isEmpty()) {
            return
        }

        preferences.setWidgetValues(widgetId, withHeaderCheckBox.isChecked,
                followGroupList[followGroupSpinner.selectedItemPosition].id, noOfRowsSpinner.selectedItemPosition + 1)
        val appWidgetManager = AppWidgetManager.getInstance(this)
        FollowWidgetProvider.updateWidgetUI(this, appWidgetManager, widgetId)
        finishWithResult()
    }

    private fun finishWithResult() {
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}