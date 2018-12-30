package com.alvinhkh.buseta.nlb.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbNews;
import com.alvinhkh.buseta.nlb.model.NlbNewsList;
import com.alvinhkh.buseta.nlb.model.NlbNewsListRequest;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class NlbNewsFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener {

    private final NlbService nlbService = NlbService.api.create(NlbService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private SwipeRefreshLayout swipeRefreshLayout;

    private NlbNewsAdapter adapter;

    private FloatingActionButton fab;

    private RecyclerView recyclerView;

    private View emptyView;

    public NlbNewsFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NlbNewsFragment newInstance() {
        return new NlbNewsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.latest_news);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(R.string.provider_nlb);
        }
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new NlbNewsAdapter(recyclerView);
        recyclerView.setAdapter(adapter);
        emptyView = rootView.findViewById(R.id.empty_view);
        TextView emptyText = rootView.findViewById(R.id.empty_text);
        emptyText.setText(R.string.message_no_notice);
        if (adapter.getDataItemCount() > 0) {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.INVISIBLE);
            emptyView.setVisibility(View.VISIBLE);
        }
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        fab = rootView.findViewById(R.id.fab);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fab != null) {
            fab.hide();
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(true);
            swipeRefreshLayout.setRefreshing(false);
        }
        onRefresh();
    }

    @Override
    public void onRefresh() {
        loadNews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                onRefresh();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
    }

    private void loadNews() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        if (adapter != null) {
            adapter.clear();
        }
        disposables.add(nlbService.getNewList(new NlbNewsListRequest("zh"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(newsListObserver()));
    }

    DisposableObserver<NlbNewsList> newsListObserver() {
        return new DisposableObserver<NlbNewsList>() {
            @Override
            public void onNext(NlbNewsList list) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(true);
                }
                if (adapter != null) {
                    if (list != null) {
                        for (NlbNews news : list.getNewses()) {
                            adapter.add(new Item(Item.TYPE_DATA, news));
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (adapter != null) {
                    adapter.setLoaded();
                    if (recyclerView != null) {
                        if (adapter.getDataItemCount() > 0) {
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.INVISIBLE);
                        }
                    }
                    if (emptyView != null) {
                        if (adapter.getDataItemCount() > 0) {
                            emptyView.setVisibility(View.GONE);
                        } else {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        };
    }
}
