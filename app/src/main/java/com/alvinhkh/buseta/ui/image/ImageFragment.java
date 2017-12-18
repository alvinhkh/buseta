package com.alvinhkh.buseta.ui.image;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.Api;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.utils.ConnectivityUtil;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;
import uk.co.senab.photoview.PhotoViewAttacher;


public class ImageFragment extends Fragment {

    public final static String ARG_TITLE = "image_title";

    public final static String ARG_URL = "image_url";

    public final static String ARG_BITMAP = "image_bitmap";

    private final CompositeDisposable disposables = new CompositeDisposable();

    private Context mContext = super.getActivity();

    private ActionBar mActionBar = null;

    private ProgressBar mProgressBar;

    private ImageView mImageView;

    private PhotoViewAttacher mPhotoViewAttacher;

    private Bitmap mBitmap = null;

    private String imageTitle;

    private String imageUrl;

    public ImageFragment() {
    }

    public static ImageFragment newInstance(String title, String url) {
        ImageFragment fragment = new ImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public static ImageFragment newInstance(String title, Bitmap bitmap) {
        ImageFragment fragment = new ImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putParcelable(ARG_BITMAP, bitmap);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        mContext = super.getActivity();
        // Get arguments
        imageTitle = getArguments().getString(ARG_TITLE);
        imageUrl = getArguments().getString(ARG_URL);
        // Overview task
        setTaskDescription(getString(R.string.notice) +
                getString(R.string.interpunct) + getString(R.string.app_name));
        // Set Toolbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (null != mActionBar) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        setHasOptionsMenu(true);
        //
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);
        // Set PhotoView
        mImageView = (ImageView) view.findViewById(R.id.image_view);
        mPhotoViewAttacher = new PhotoViewAttacher(mImageView);
        mPhotoViewAttacher.setMaximumScale(4);
        // Text View
        TextView mTextView = (TextView) view.findViewById(android.R.id.text1);
        mTextView.setOnClickListener(v -> {
            if (mPhotoViewAttacher != null)
                mPhotoViewAttacher.setScale(1);
        });
        if (!TextUtils.isEmpty(imageTitle)) {
            mTextView.setText(imageTitle);
            mTextView.setVisibility(View.VISIBLE);
        } else {
            mTextView.setVisibility(View.GONE);
        }

        showNoticeImage(imageUrl);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, imageTitle);
        outState.putString(ARG_URL, imageUrl);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mActionBar) {
            mActionBar.setTitle(R.string.notice);
            mActionBar.setSubtitle(null);
        }
    }

    @Override
    public void onDestroyView() {
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.cleanup();
        }
        if (mImageView != null) {
            mImageView.setImageBitmap(null);
            mImageView.destroyDrawingCache();
        }
        View view = getView();
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        disposables.clear();
        super.onDestroyView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            showNoticeImage(imageUrl);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTaskDescription(String title) {
        // overview task
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        ActivityManager.TaskDescription taskDesc =
                new ActivityManager.TaskDescription(title, bm,
                        ContextCompat.getColor(mContext, R.color.colorPrimary600));
        ((AppCompatActivity) mContext).setTaskDescription(taskDesc);
    }

    private void showNoticeImage(String url) {
        if(!URLUtil.isValidUrl(url)) {
            Toast.makeText(getContext(), R.string.missing_input, Toast.LENGTH_SHORT).show();
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
            return;
        }
        // Check internet connection
        if (!ConnectivityUtil.isConnected(mContext)) {
            Snackbar.make(getActivity().findViewById(android.R.id.content),
                    R.string.message_no_internet_connection, Snackbar.LENGTH_LONG).show();
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
            return;
        }

        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        disposables.add(Api.raw.create(Api.class).get(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(getImage()));
    }

    DisposableObserver<ResponseBody> getImage() {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                if (body == null) return;
                String contentType = body.contentType().toString();
                if (contentType.contains("image")) {
                    mBitmap = BitmapFactory.decodeStream(body.byteStream());
                } else {
                    Timber.d(contentType);
                }
            }

            @Override
            public void onError(Throwable e) {
                mBitmap = null;
                Timber.d(e);
            }

            @Override
            public void onComplete() {
                if (mImageView != null && mBitmap != null) {
                    mImageView.setImageBitmap(mBitmap);
                    mPhotoViewAttacher.update();
                }
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        };
    }

}