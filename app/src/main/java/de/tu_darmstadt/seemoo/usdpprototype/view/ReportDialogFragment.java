package de.tu_darmstadt.seemoo.usdpprototype.view;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by kenny on 18.02.16.
 */
public class ReportDialogFragment extends DialogFragment {

    protected Bundle bundle = null;
    protected String title = "report dialog";
    protected String info = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public boolean isFragmentUIActive() {
        return isAdded() && !isDetached() && !isRemoving();
    }
}
