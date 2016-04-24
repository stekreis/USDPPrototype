package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by kenny on 18.02.16.
 */
public class AuthDialogFragment extends DialogFragment {

    public static final String AUTH_PATTERN = "AUTH_PATTERN";
    public static final String AUTH_TITLE = "AUTH_TITLE";
    public static final String AUTH_INFO = "AUTH_INFO";
    public static final String AUTH_MECHTYPE = "AUTH_MECHTYPE";
    public static final String AUTH_EXPLINFO = "AUTH_EXPLINFO";

    public static final String AUTH_TARGET_DVC = "AUTHTRGT_DVC";

    protected String tgtDevice = "";
    protected String mechType = "";
    protected Bundle bundle = null;
    protected String title = "Authentication dialog";
    protected String info = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bundle = getArguments();
        String pTitle = bundle.getString(AUTH_TITLE);
        if (pTitle != null) {
            title = pTitle;
        }
        String pInfo = bundle.getString(AUTH_INFO);
        if (pInfo != null) {
            info = pInfo;
        }

    }

    public interface oobResultInterface {
        void oobResult(String targetDevice, String mech, boolean result);
        void oobResult(String targetDevice, String mech, String data);
    }

    public boolean isFragmentUIActive() {
        return isAdded() && !isDetached() && !isRemoving();
    }
}
