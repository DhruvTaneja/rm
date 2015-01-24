package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dhruv.resumemanager.NavDrawerItem;
import com.dhruv.resumemanager.R;

import java.util.ArrayList;

/**
 * Created by dhruv on 3/1/15.
 * This class is the custom list adapter
 * for the drawer list
 */
public class NavDrawerListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<NavDrawerItem> navDrawerItems;

    public NavDrawerListAdapter(Context context, ArrayList<NavDrawerItem> navDrawerItems) {
        this.context = context;
        this.navDrawerItems = navDrawerItems;
    }

    @Override
    public int getCount() {
        return navDrawerItems.size();
    }

    @Override
    public Object getItem(int position) {
        return navDrawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            LayoutInflater layoutInflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_view_row, null);
        }

        ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView count = (TextView) convertView.findViewById(R.id.counter);

        icon.setImageResource(navDrawerItems.get(position).getIcon());
        title.setText(navDrawerItems.get(position).getTitle());

        if(navDrawerItems.get(position).getCounterVisibility())
            count.setText(navDrawerItems.get(position).getCount());
        else
            count.setVisibility(View.GONE);

        return convertView;
    }
}
