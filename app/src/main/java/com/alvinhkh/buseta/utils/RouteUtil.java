package com.alvinhkh.buseta.utils;

import android.content.Context;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

public class RouteUtil {

    public static String getCompanyName(@NonNull Context context,
                                        @NonNull String companyCode,
                                        @Nullable String routeNo) {
        if (TextUtils.isEmpty(companyCode)) return "";
        String companyName = companyCode;
        switch (companyCode) {
            case C.PROVIDER.AESBUS:
                companyName = context.getString(R.string.provider_short_aes_bus);
                break;
            case C.PROVIDER.CTB:
                companyName = context.getString(R.string.provider_short_ctb);
                break;
            case C.PROVIDER.KMB:
                companyName = context.getString(R.string.provider_short_kmb);
                if (!TextUtils.isEmpty(routeNo)) {
                    if (routeNo.startsWith("NR")) {
                        companyName = context.getString(R.string.provider_short_residents);
                    } else if (routeNo.startsWith("A") || routeNo.startsWith("E") || routeNo.startsWith("NA")) {
                        companyName = context.getString(R.string.provider_short_lwb);
                    }
                }
                break;
            case C.PROVIDER.LRTFEEDER:
                companyName = context.getString(R.string.provider_short_lrtfeeder);
                break;
            case C.PROVIDER.MTR:
                companyName = context.getString(R.string.provider_short_mtr);
                break;
            case C.PROVIDER.NLB:
                companyName = context.getString(R.string.provider_short_nlb);
                break;
            case C.PROVIDER.NWFB:
                companyName = context.getString(R.string.provider_short_nwfb);
                break;
            case C.PROVIDER.NWST:
                companyName = context.getString(R.string.provider_short_nwst);
                break;
        }
        return companyName;
    }
}
