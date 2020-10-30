package com.alvinhkh.buseta.follow.ui

import androidx.lifecycle.ViewModelProvider
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.follow.model.FollowGroup
import com.alvinhkh.buseta.ui.OnItemDragListener
import com.alvinhkh.buseta.ui.SimpleItemTouchHelperCallback
import com.alvinhkh.buseta.utils.ColorUtil
import com.jaredrummler.android.colorpicker.ColorPickerView


class EditFollowFragment: Fragment(), OnItemDragListener {

    private lateinit var followDatabase: FollowDatabase
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var viewAdapter: EditFollowViewAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private var groupId = FollowGroup.UNCATEGORISED
    private var dragItemPosition = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_follow, container, false)
        setHasOptionsMenu(true)
        groupId = arguments?.getString(C.EXTRA.GROUP_ID)?:FollowGroup.UNCATEGORISED
        followDatabase = FollowDatabase.getInstance(rootView.context)!!
        emptyView = rootView.findViewById(R.id.empty_view)
        emptyView.visibility = View.GONE
        recyclerView = rootView.findViewById(R.id.recycler_view)
        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            viewAdapter = EditFollowViewAdapter(this, this@EditFollowFragment)
            adapter = viewAdapter
            itemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(viewAdapter))
            itemTouchHelper.attachToRecyclerView(this)
        }
        val viewModel = ViewModelProvider(this).get(FollowViewModel::class.java)
        viewModel.liveData(groupId).observe(viewLifecycleOwner, { list ->
            viewAdapter.replaceItems(list?: mutableListOf())
            emptyView.visibility = if (viewAdapter.itemCount > 0) View.GONE else View.VISIBLE
        })
        followDatabase.followGroupDao().liveData(groupId)
                .observe(viewLifecycleOwner, { followGroup ->
                    if (activity != null) {
                        val actionBar = (activity as AppCompatActivity).supportActionBar
                        val name = when {
                            followGroup?.id == FollowGroup.UNCATEGORISED -> getString(R.string.uncategorised)
                            !followGroup?.name.isNullOrEmpty() -> followGroup?.name
                            else -> followGroup?.id
                        }?:""
                        actionBar?.title = name
                        actionBar?.subtitle = getString(R.string.edit_follow_group)
                        val color = if (!followGroup?.colour.isNullOrEmpty()) Color.parseColor(followGroup?.colour) else ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                        (activity as AppCompatActivity).supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                            activity?.window?.statusBarColor = color
                            activity?.window?.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.transparent)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            activity?.window?.statusBarColor = ColorUtil.darkenColor(color)
                            activity?.window?.navigationBarColor = ColorUtil.darkenColor(color)
                        }
                        activity?.findViewById<FrameLayout>(R.id.adView_container)?.setBackgroundColor(color)
                    }
                })
        val buttonContainer = rootView.findViewById<View>(R.id.button_container)
        buttonContainer.visibility = View.VISIBLE
        val editButton = rootView.findViewById<MaterialButton>(R.id.edit_button)
        editButton.setOnClickListener { view ->
            val followGroup = followDatabase.followGroupDao().get(groupId)?: return@setOnClickListener
            val builder = AlertDialog.Builder(view.context, R.style.AppTheme_Dialog)
            builder.setTitle(R.string.rename)
            val layout = LinearLayout(builder.context)
            layout.setPadding(48, 0, 48, 0)
            layout.orientation = LinearLayout.VERTICAL
            val textInputLayout = TextInputLayout(builder.context)
            val textInputEditText = TextInputEditText(builder.context)
            textInputEditText.setText(followGroup.name)
            textInputLayout.addView(textInputEditText)
            layout.addView(textInputLayout)
            builder.setView(layout)
            builder.setNegativeButton(R.string.action_cancel) { dialogInterface, _ -> dialogInterface.cancel() }
            builder.setPositiveButton(R.string.action_confirm) { dialogInterface, _ ->
                val name = textInputEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newGroup = followGroup.copy()
                    newGroup.name = name
                    followDatabase.followGroupDao().updateAll(newGroup)
                }
                dialogInterface.dismiss()
            }
            builder.create().show()
        }
        val colourButton = rootView.findViewById<MaterialButton>(R.id.colour_button)
        colourButton.setOnClickListener { view ->
            val followGroup = followDatabase.followGroupDao().get(groupId)?: return@setOnClickListener
            val builder = AlertDialog.Builder(view.context, R.style.AppTheme_Dialog)
            builder.setTitle(R.string.colour)
            val layout = LinearLayout(builder.context)
            layout.setPadding(48, 0, 48, 0)
            layout.orientation = LinearLayout.VERTICAL
            val colorPickerView = ColorPickerView(builder.context)
            colorPickerView.setAlphaSliderVisible(false)
            colorPickerView.color = if (!followGroup.colour.isEmpty())
                Color.parseColor(followGroup.colour)
            else
                ContextCompat.getColor(builder.context, R.color.colorPrimary)
            layout.addView(colorPickerView)
            val textInputLayout = TextInputLayout(builder.context)
            val textInputEditText = TextInputEditText(builder.context)
            textInputEditText.maxLines = 1
            textInputEditText.setText(String.format("%06X", 0xFFFFFF and colorPickerView.color))
            textInputEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    try {
                        colorPickerView.color = Color.parseColor("#$s")
                    } catch (ignored: Throwable) {}
                }
            })
            textInputLayout.addView(textInputEditText)
            layout.addView(textInputLayout)
            colorPickerView.setOnColorChangedListener { color ->
                textInputEditText.setText(String.format("%06X", 0xFFFFFF and color))
            }
            builder.setView(layout)
            builder.setPositiveButton(getString(R.string.action_confirm)) { dialogInterface, _ ->
                val newGroup = followGroup.copy()
                newGroup.colour = String.format("#%06X", 0xFFFFFF and colorPickerView.color)
                followDatabase.followGroupDao().updateAll(newGroup)
                dialogInterface.dismiss()
            }
            builder.setNeutralButton(R.string.default_value) { dialogInterface, _ ->
                val newGroup = followGroup.copy()
                newGroup.colour = ""
                followDatabase.followGroupDao().updateAll(newGroup)
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(R.string.action_cancel) { dialogInterface, _ -> dialogInterface.dismiss() }
            builder.show()
        }
        val deleteButton = rootView.findViewById<MaterialButton>(R.id.delete_button)
        deleteButton.setOnClickListener { view ->
            val followGroup = followDatabase.followGroupDao().get(groupId)?: return@setOnClickListener
            val builder = AlertDialog.Builder(view.context, R.style.AppTheme_Dialog)
            builder.setTitle(R.string.remove_group)
            builder.setMessage(followGroup.name)
            builder.setNegativeButton(R.string.action_cancel) { dialogInterface, _ -> dialogInterface.cancel() }
            builder.setPositiveButton(R.string.action_confirm) { dialogInterface, _ ->
                followDatabase.followGroupDao().delete(followGroup.id)
                followDatabase.followDao().delete(followGroup.id)
                dialogInterface.dismiss()
                val fragmentManager = activity?.supportFragmentManager!!
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragment_container, EditFollowGroupFragment.newInstance())
                fragmentTransaction.addToBackStack("edit_follow_list")
                fragmentTransaction.commit()
            }
            builder.create().show()
        }
        val group = followDatabase.followGroupDao().get(groupId)
        if (group?.id?:"" == FollowGroup.UNCATEGORISED) {
            editButton.visibility = View.INVISIBLE
            deleteButton.visibility = View.INVISIBLE
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            fab?.hide()
        }
        if (view != null) {
            Snackbar.make(requireView(), R.string.swipe_to_remove_follow_stop, Snackbar.LENGTH_SHORT).show()
        }
        viewAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_refresh) {
            viewAdapter.notifyDataSetChanged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
        dragItemPosition = viewHolder.adapterPosition
    }

    override fun onItemStopDrag(viewHolder: RecyclerView.ViewHolder) {
        if (dragItemPosition >= 0 && dragItemPosition != viewHolder.adapterPosition) {
            val fromPosition = dragItemPosition
            val toPosition = viewHolder.adapterPosition
            val updateItems = arrayListOf<Follow>()
            followDatabase.followDao().list(groupId).forEachIndexed { index, follow ->
                var update = false
                if (fromPosition < toPosition) {  // down
                    if (index > fromPosition - 1) {
                        update = true
                    }
                } else if (fromPosition > toPosition) {  // up
                    if (index > toPosition - 1) {
                        update = true
                    }
                }
                if (update) {
                    var i = index
                    when {
                        index == fromPosition -> i = toPosition
                        fromPosition < toPosition -> // down
                            i = index - 1
                        fromPosition > toPosition -> // up
                            i = index + 1
                    }
                    follow.order = i
                    updateItems.add(follow)
                }
            }
            followDatabase.followDao().updateAll(*updateItems.toTypedArray())
        }
        dragItemPosition = -1
    }

    companion object {

        fun newInstance(groupId: String): EditFollowFragment {
            val fragment = EditFollowFragment()
            val args = Bundle()
            args.putString(C.EXTRA.GROUP_ID, groupId)
            fragment.arguments = args
            return fragment
        }
    }
}
