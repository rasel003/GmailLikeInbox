package com.rasel.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.content.res.TypedArray
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener,
    MessagesAdapter.MessageAdapterListener {
    private val messages = ArrayList<Message>()
    private var recyclerView: RecyclerView? = null
    private var mAdapter: MessagesAdapter? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var actionModeCallback: ActionModeCallback? = null
    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        })

        recyclerView = findViewById(R.id.recycler_view) as RecyclerView
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout) as SwipeRefreshLayout
        swipeRefreshLayout!!.setOnRefreshListener(this)

        mAdapter = MessagesAdapter(this, messages, this)
        val mLayoutManager = LinearLayoutManager(applicationContext)
        recyclerView!!.layoutManager = mLayoutManager
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        recyclerView!!.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        recyclerView!!.adapter = mAdapter

        actionModeCallback = ActionModeCallback()

        // show loader and fetch messages
        swipeRefreshLayout!!.post { getInbox() }
    }

    /**
     * Fetches mail messages by making HTTP request
     * url: https://api.androidhive.info/json/inbox.json
     */
    private fun getInbox() {
        swipeRefreshLayout!!.isRefreshing = true

        val apiService = ApiClient.client.create(ApiInterface::class.java)

        val call = apiService.inbox
        call.enqueue(object : Callback<List<Message>> {
            override fun onResponse(call: Call<List<Message>>, response: Response<List<Message>>) {
                // clear the inbox
                messages.clear()

                // add all the messages
                // messages.addAll(response.body());

                // TODO - avoid looping
                // the loop was performed to add colors to each message
                for (message in response.body()!!) {
                    // generate a random color
                    message.color = getRandomMaterialColor("400")
                    messages.add(message)
                }

                mAdapter!!.notifyDataSetChanged()
                swipeRefreshLayout!!.isRefreshing = false
            }

            override fun onFailure(call: Call<List<Message>>, t: Throwable) {
                Toast.makeText(
                    applicationContext,
                    "Unable to fetch json: " + t.message,
                    Toast.LENGTH_LONG
                ).show()
                swipeRefreshLayout!!.isRefreshing = false
            }
        })
    }

    /**
     * chooses a random color from array.xml
     */
    private fun getRandomMaterialColor(typeColor: String): Int {
        var returnColor = Color.GRAY
        val arrayId = resources.getIdentifier("mdcolor_$typeColor", "array", packageName)

        if (arrayId != 0) {
            val colors = resources.obtainTypedArray(arrayId)
            val index = (Math.random() * colors.length()).toInt()
            returnColor = colors.getColor(index, Color.GRAY)
            colors.recycle()
        }
        return returnColor
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.getItemId()


        if (id == R.id.action_search) {
            Toast.makeText(applicationContext, "Search...", Toast.LENGTH_SHORT).show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRefresh() {
        // swipe refresh is performed, fetch the messages again
        getInbox()
    }

    override fun onIconClicked(position: Int) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback!!)
        }

        toggleSelection(position)
    }

    override fun onIconImportantClicked(position: Int) {
        // Star icon is clicked,
        // mark the message as important
        val message = messages.get(position)
        message.isImportant = !message.isImportant
        messages.set(position, message)
        mAdapter!!.notifyDataSetChanged()
    }

    override fun onMessageRowClicked(position: Int) {
        // verify whether action mode is enabled or not
        // if enabled, change the row state to activated
        if (mAdapter!!.selectedItemCount > 0) {
            enableActionMode(position)
        } else {
            // read the message which removes bold from the row
            val message = messages.get(position)
            message.isRead = true
            messages.set(position, message)
            mAdapter!!.notifyDataSetChanged()

            Toast.makeText(applicationContext, "Read: " + message.message!!, Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onRowLongClicked(position: Int) {
        // long press is performed, enable action mode
        enableActionMode(position)
    }

    private fun enableActionMode(position: Int) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback!!)
        }
        toggleSelection(position)
    }

    private fun toggleSelection(position: Int) {
        mAdapter!!.toggleSelection(position)
        val count = mAdapter!!.selectedItemCount

        if (count == 0) {
            actionMode!!.finish()
        } else {
            actionMode!!.setTitle(count.toString())
            actionMode!!.invalidate()
        }
    }


    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu)

            // disable swipe refresh if action mode is enabled
            swipeRefreshLayout!!.isEnabled = false
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.getItemId()) {
                R.id.action_delete -> {
                    // delete all the selected messages
                    deleteMessages()
                    mode.finish()
                    return true
                }

                else -> return false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            mAdapter!!.clearSelections()
            swipeRefreshLayout!!.isEnabled = true
            actionMode = null
            recyclerView!!.post {
                mAdapter!!.resetAnimationIndex()
                // mAdapter.notifyDataSetChanged();
            }
        }
    }

    // deleting the messages from recycler view
    private fun deleteMessages() {
        mAdapter!!.resetAnimationIndex()
        val selectedItemPositions = mAdapter!!.getSelectedItems()
        for (i in selectedItemPositions.indices.reversed()) {
            mAdapter!!.removeData(selectedItemPositions[i])
        }
        mAdapter!!.notifyDataSetChanged()
    }
}
