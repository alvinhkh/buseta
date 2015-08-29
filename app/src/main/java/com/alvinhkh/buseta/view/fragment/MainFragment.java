package com.alvinhkh.buseta.view.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.FavouriteDatabase;
import com.alvinhkh.buseta.view.adapter.FeatureAdapter;
import com.alvinhkh.buseta.database.SuggestionsDatabase;

public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    private Context mContext = super.getActivity();
    private SuggestionsDatabase mDatabase_suggestion;
    private FavouriteDatabase mDatabase_favourite;
    private FeatureAdapter mAdapter;
    private ActionBar mActionBar = null;
    private RecyclerView mRecyclerView;
    private MenuItem mSearchMenuItem;
    private UpdateHistoryReceiver mReceiver;

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
        mDatabase_suggestion = new SuggestionsDatabase(getActivity().getApplicationContext());
        mDatabase_favourite = new FavouriteDatabase(getActivity().getApplicationContext());
        // Toolbar
        mActionBar = ((AppCompatActivity) mContext).getSupportActionBar();
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setSubtitle(null);
        mActionBar.setDisplayHomeAsUpEnabled(false);
        setHasOptionsMenu(true);

        Button mButton = (Button) view.findViewById(R.id.buttonSearch);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mSearchMenuItem) {
                    mSearchMenuItem.expandActionView();
                }
            }
        });

        mRecyclerView = (RecyclerView) view.findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        final GridLayoutManager manager = new GridLayoutManager(mContext, 2);
        mRecyclerView.setLayoutManager(manager);
        mAdapter = new FeatureAdapter(getActivity(),
                mDatabase_suggestion.getHistory(),
                mDatabase_favourite.get());
        mRecyclerView.setAdapter(mAdapter);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < mAdapter.getFavouriteCount() ? manager.getSpanCount() : 1;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActionBar != null) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
        }
        if (null != mAdapter) {
            mAdapter.swapHistoryCursor(mDatabase_suggestion.getHistory());
        }
        if (null != mContext) {
            IntentFilter mFilter = new IntentFilter(Constants.MESSAGE.HISTORY_UPDATED);
            mReceiver = new UpdateHistoryReceiver();
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
        if (null != mDatabase_suggestion)
            mDatabase_suggestion.close();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_settings).setVisible(true);
        mSearchMenuItem = menu.findItem(R.id.action_search);
    }

    public class UpdateHistoryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.HISTORY_UPDATED);
            if (null != mAdapter && null != mDatabase_suggestion && aBoolean == true) {
                mAdapter.swapHistoryCursor(mDatabase_suggestion.getHistory());
            }
        }
    }

}
