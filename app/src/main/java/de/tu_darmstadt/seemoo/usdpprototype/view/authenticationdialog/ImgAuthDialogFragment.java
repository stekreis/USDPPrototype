package de.tu_darmstadt.seemoo.usdpprototype.view.authenticationdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.secondarychannel.OOBData;
import de.tu_darmstadt.seemoo.usdpprototype.view.UsdpMainActivity;

/**
 * Created by kenny on 15.02.16.
 */
public class ImgAuthDialogFragment extends AuthDialogFragment {

    public static final String AUTH_VICP = "AUTH_VICP";
    public static final String IMG_WIDTH = "IMG_WIDTH";
    public static final String IMG_HEIGHT = "IMG_HEIGHT";
    public static final String IMG_IMAGE = "IMG_IMAGE";
    private static final String LOGTAG = "AuthImgDialogFrag";

    private String mechType = "";

    private Bitmap image = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "created");

    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        View view = layoutInflater.inflate(R.layout.dialog_auth_img, null);

        TextView tv_title = (TextView) view.findViewById(R.id.tv_authdialog_title);
        tv_title.setText(title);
        TextView tv_info = (TextView) view.findViewById(R.id.tv_authdialog_info);
        tv_info.setText(info);

        ImageView iv_image = (ImageView) view.findViewById(R.id.iv_image);
        int width = bundle.getInt(IMG_WIDTH);
        int height = bundle.getInt(IMG_HEIGHT);
        mechType = bundle.getString(AUTH_MECHTYPE);
        image = Bitmap.createBitmap((int[]) getArguments().get(IMG_IMAGE), 0, width, width, height, Bitmap.Config.ARGB_8888);
        iv_image.setImageBitmap(image);

        builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                activity.oobResult(mechType, true);
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                UsdpMainActivity activity = (UsdpMainActivity) getActivity();
                activity.oobResult(mechType, false);
                ImgAuthDialogFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }


}