package de.tu_darmstadt.seemoo.usdpprototype.view;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.tu_darmstadt.seemoo.usdpprototype.R;
import de.tu_darmstadt.seemoo.usdpprototype.authentication.AuthMechanism;

/**
 * Created by kenny on 19.04.16.
 */
public class AuthMechArrayAdapter extends ArrayAdapter<AuthMechanism> {

    Context context;
    int layoutResourceId;
    AuthMechanism data[] = null;

    public AuthMechArrayAdapter(Context context, int layoutResourceId, AuthMechanism[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        MechHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new MechHolder();
            //holder.imgIcon = (ImageView)row.findViewById(R.id.iv_authmechrow);
            holder.txtTitle = (TextView)row.findViewById(R.id.tv_mechname);
            holder.mechVal = (TextView)row.findViewById(R.id.tv_mechval);

            row.setTag(holder);
        }
        else
        {
            holder = (MechHolder)row.getTag();
        }

        AuthMechanism mech = data[position];
        holder.txtTitle.setText(mech.getShortName());
        holder.mechVal.setText(mech.getValuation() + " mechVal");
        //holder.imgIcon.setImageResource(R.drawable.mech_sib);

        return row;
    }

    static class MechHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
        TextView mechVal;
    }
}