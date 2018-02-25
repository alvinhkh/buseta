package com.alvinhkh.buseta.datagovhk.ui;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.datagovhk.DataGovHkService;
import com.alvinhkh.buseta.datagovhk.model.MtrLineStation;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item.TYPE_DATA;


public class MtrLineStationsFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener,
        ArrayListRecyclerViewAdapter.OnClickItemListener {

    private final DataGovHkService dataGovHkService = DataGovHkService.resource.create(DataGovHkService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private SwipeRefreshLayout swipeRefreshLayout;

    private MtrLineStationsAdapter adapter;

    private FloatingActionButton fab;

    private RecyclerView recyclerView;

    private View emptyView;

    private Map<String, String> codeMap = new HashMap<>();

    private String lineCode = "";

    public MtrLineStationsFragment() { }

    protected final Handler refreshHandler = new Handler();

    protected final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (adapter != null) {
                if (getContext() != null) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    if (preferences != null && preferences.getBoolean("load_etas", false)) {
                        onRefresh();
                        refreshHandler.postDelayed(this, 60000);  // refresh every 60 sec
                        return;
                    }
                }
                adapter.notifyDataSetChanged();
            }
            refreshHandler.postDelayed(this, 30000);  // refresh every 30 sec
        }
    };

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MtrLineStationsFragment newInstance(@NonNull String lineCode) {
        MtrLineStationsFragment fragment = new MtrLineStationsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(C.EXTRA.LINE_CODE, lineCode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.ETA_UPDATE))
                .share().subscribeWith(etaObserver()));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.provider_mtr);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(null);
        }
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new MtrLineStationsAdapter(recyclerView, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
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
        onRefresh();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void setUserVisibleHint(boolean isUserVisible) {
        super.setUserVisibleHint(isUserVisible);
        if (getView() != null) {
            if (isUserVisible) {
                if (getActivity() != null) {
                    FloatingActionButton fab = getActivity().findViewById(R.id.fab);
                    if (fab != null) {
                        fab.setOnClickListener(v -> onRefresh());
                    }
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
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
    public void onRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        if (adapter != null) {
            adapter.clear();
        }
        lineCode = "";
        if (getArguments() != null) {
            lineCode = getArguments().getString(C.EXTRA.LINE_CODE);
        }
        if (TextUtils.isEmpty(lineCode)) {
            Toast.makeText(getContext(), R.string.missing_input, Toast.LENGTH_SHORT).show();
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
            return;
        }
        codeMap.clear();
        disposables.add(dataGovHkService.mtrLinesAndStations()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(mtrLinesAndStationsObserver(lineCode)));
    }

    @Override
    public void onClickItem(Item item, int position) {
        if (item.getType() == Item.TYPE_DATA) {
            RouteStop station = (RouteStop) item.getObject();
            if (station != null && getContext() != null) {
                ArrayList<Route> routes = new ArrayList<>();
                for (String key: codeMap.keySet()) {
                    Route route = new Route();
                    route.setCode(key);
                    route.setName(codeMap.get(key));
                    routes.add(route);
                }
                Intent intent = new Intent(getContext(), EtaService.class);
                intent.putExtra(C.EXTRA.ROUTE_LIST, routes);
                intent.putExtra(C.EXTRA.STOP_OBJECT, station);
                getContext().startService(intent);
            }
        }
    }

    DisposableObserver<Intent> etaObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                RouteStop routeStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (routeStop == null) return;
                if (!routeStop.getRouteId().equals(lineCode)) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED) || bundle.getBoolean(C.EXTRA.FAIL)) {
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    int i = 0;
                    for (Item item : adapter.getItems()) {
                        if (item.getType() == TYPE_DATA && ((RouteStop) item.getObject()).getSequence().equals(routeStop.getSequence())) {
                            adapter.notifyItemChanged(i);
                            break;
                        }
                        i++;
                    }
                } else if (bundle.getBoolean(C.EXTRA.UPDATING)) {
                    if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(true);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
            }
        };
    }

    DisposableObserver<ResponseBody> mtrLinesAndStationsObserver(String lineCode) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                if (body == null || adapter == null) return;
                try {
                    List<MtrLineStation> stations = MtrLineStation.Companion.fromCSV(body.string(), lineCode);
                    for (MtrLineStation station: stations) {
                        RouteStop routeStop = RouteStopUtil.fromMtrLineStation(station);
                        if (!codeMap.containsKey(station.getStationCode())) {
                            adapter.add(new Item(Item.TYPE_DATA, routeStop));
                            codeMap.put(station.getStationCode(), station.getChineseName());
                        }
                    }
                } catch (IOException e) {
                    Timber.d(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                getActivity().runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onComplete() {
                getActivity().runOnUiThread(() -> {
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
                            recyclerView.setItemViewCacheSize(Math.min(adapter.getItemCount(), 5));
                        }
                        if (emptyView != null) {
                            if (adapter.getDataItemCount() > 0) {
                                emptyView.setVisibility(View.GONE);
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            }
        };
    }
}
