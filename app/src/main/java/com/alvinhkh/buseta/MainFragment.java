package com.alvinhkh.buseta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    private Context mContext = super.getActivity();
    private SuggestionsDatabase mDatabase;
    private SearchHistoryAdapter mAdapter;
    private ActionBar mActionBar = null;
    private RecyclerView mRecyclerView;
    private MenuItem mSearchMenuItem;
    private UpdateViewReceiver mReceiver;

    public MainFragment() {
    }

    public static MainFragment newInstance() {
        MainFragment f = new MainFragment();
        Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContext = super.getActivity();
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mDatabase = new SuggestionsDatabase(getActivity().getApplicationContext());
        // Toolbar
        mActionBar = ((AppCompatActivity) mContext).getSupportActionBar();
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setSubtitle(null);
        mActionBar.setDisplayHomeAsUpEnabled(false);
        setHasOptionsMenu(true);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));

        Cursor mCursor = mDatabase.getHistory();
        mAdapter = new SearchHistoryAdapter(getActivity(), mCursor);
        mRecyclerView.setAdapter(mAdapter);

        Button mButton = (Button) view.findViewById(R.id.buttonSearch);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mSearchMenuItem) {
                    mSearchMenuItem.expandActionView();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActionBar != null) {
            mActionBar.setSubtitle(null);
        }
        if (null != mContext) {
            IntentFilter mFilter = new IntentFilter(Constants.MESSAGE.HISTORY_UPDATED);
            mReceiver = new UpdateViewReceiver();
            mFilter.addAction(Constants.MESSAGE.HISTORY_UPDATED);
            mContext.registerReceiver(mReceiver, mFilter);
        }
    }

    @Override
    public void onPause() {
        if (null != mContext && null != mReceiver)
            mContext.unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (null != mDatabase)
            mDatabase.close();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_clear_history).setVisible(true);
        menu.findItem(R.id.action_about).setVisible(true);
        mSearchMenuItem = menu.findItem(R.id.action_search);
    }

    public class UpdateViewReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.HISTORY_UPDATED);
            if (null != mAdapter && null != mDatabase && aBoolean == true) {
                Cursor mCursor = mDatabase.getHistory();
                mAdapter.changeCursor(mCursor);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

}
