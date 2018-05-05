package com.alvinhkh.buseta.mtr.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.datagovhk.model.MtrLineName;
import com.alvinhkh.buseta.mtr.MtrService;
import com.alvinhkh.buseta.mtr.model.MtrLineStatus;
import com.alvinhkh.buseta.mtr.model.MtrLineStatusRes;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class MtrLineStatusFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener {

    private final MtrService mtrService = MtrService.Companion.getTnews().create(MtrService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private SwipeRefreshLayout swipeRefreshLayout;

    private MtrLineStatusAdapter adapter;

    private FloatingActionButton fab;

    private RecyclerView recyclerView;

    private View emptyView;

    private Map<String, MtrLineName> codeMap = new HashMap<>();

    public MtrLineStatusFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MtrLineStatusFragment newInstance() {
        return new MtrLineStatusFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new MtrLineStatusAdapter(recyclerView);
        recyclerView.setAdapter(adapter);
        emptyView = rootView.findViewById(R.id.empty_view);
        TextView emptyText = rootView.findViewById(R.id.empty_text);
        emptyText.setText(R.string.message_no_data);
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
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setRefreshing(false);
        }
        if (getActivity() != null) {
            FloatingActionButton fab = getActivity().findViewById(R.id.fab);
            if (fab != null) {
                fab.hide();
            }
        }
        onRefresh();
    }

    @Override
    public void onRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        if (adapter != null) {
            adapter.clear();
        }
        codeMap.clear();
        if (getActivity() != null && getActivity().getAssets() != null) try {
            InputStream inputStream = getActivity().getAssets().open("mtr_lines_name.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            MtrLineName[] mtrLineNameArray = gson.fromJson(json, MtrLineName[].class);
            List<MtrLineName> mtrLineNames = Arrays.asList(mtrLineNameArray);
            for (MtrLineName mtrLineName : mtrLineNames) {
                codeMap.put(mtrLineName.getLineCode(), mtrLineName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        disposables.add(mtrService.lineStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(lineStatusObserver()));
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

    DisposableObserver<MtrLineStatusRes> lineStatusObserver() {
        return new DisposableObserver<MtrLineStatusRes>() {
            @Override
            public void onNext(MtrLineStatusRes res) {
                if (res == null) return;
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(true);
                }
                if (adapter != null) {
                    if (res.getLines() != null) {
                        for (MtrLineStatus status : res.getLines()) {
                            status.setLineName(status.getLineCode());
                            if (!codeMap.containsKey(status.getLineCode())) continue;
                            MtrLineName mtrLineName = codeMap.get(status.getLineCode());
                            status.setLineName(mtrLineName.getNameTc());
                            status.setLineColour(mtrLineName.getColour());
                            adapter.add(new Item(Item.TYPE_DATA, status));
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
