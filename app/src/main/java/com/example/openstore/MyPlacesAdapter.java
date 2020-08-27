package com.example.openstore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MyPlacesAdapter extends BaseAdapter implements Filterable {

    private LayoutInflater mInflater;
    Context con;
    LatLng latLng;
    ArrayList<MyGooglePlaces> places=new ArrayList<MyGooglePlaces>();
    public MyPlacesAdapter(Context context, LatLng latlng) {
        super();
        mInflater = LayoutInflater.from(context);
        con=context;
        latLng = latlng;
    }
    // get total count of array
    @Override
    public int getCount() {
        return places.size();
    }

    //get item id
    @Override
    public long getItemId(int position) {
        return position;
    }
    // get Mygoogleplaces object at given position
    @Override
    public MyGooglePlaces getItem(int position) {
        return places.get(position);
    }
    //Filter to filter the results
    @Override
    public Filter getFilter() {
        Filter filter=new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results=new FilterResults();
                if(constraint!=null)
                {
                    places=getPredictions(constraint.toString(),latLng);
                    if(places!=null)
                    {
                        results.values=places;
                        results.count=places.size();
                    }
                }
                return results;
            }
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    // The API returned at least one result, update the data.
                    notifyDataSetChanged();
                } else {
                    // The API did not return any results, invalidate the data set.
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
    // method to get different places nearby search location
    private ArrayList<MyGooglePlaces> getPredictions(String constraint, LatLng latLng)
    {
        //pass your current latitude and longitude to find nearby and ranky=distance means places will be found in ascending order
        // according to distance
        String API_KEY="AIzaSyCdv5hmYW2q7_AgyaruClyb5iKv1oUQSNw";
        String url= "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+latLng.latitude+","+latLng.longitude+"&rankby=distance&name="+constraint+"&key="+API_KEY;
        return getPlaces(url);
    }
    private ArrayList<MyGooglePlaces> getPlaces(String constraint)
    {
        //code for API level 23 as httpclient is depricated in API 23
        StringBuilder sb= new StringBuilder();
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(constraint);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-length","0");
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.connect();
            int status = urlConnection.getResponseCode();

            switch(status) {
                case 200:
                case 201:
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader isw = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isw);
                    String data;
                    while ((data = br.readLine()) != null) {
                        sb.append(data + "\n");
                    }
                    br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return   parseGoogleParse(sb.toString());
    }
    // method to parse the json returned by googleplaces api
    private static ArrayList parseGoogleParse(final String response) {
        ArrayList<MyGooglePlaces> temp = new ArrayList();
        try {
            // make an jsonObject in order to parse the response
            JSONObject jsonObject = new JSONObject(response);
            // make an jsonObject in order to parse the response
            if (jsonObject.has("results")) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                for (int i = 0; i < jsonArray.length(); i++) {
                    MyGooglePlaces poi = new MyGooglePlaces();
                    if (jsonArray.getJSONObject(i).has("name")) {
                        poi.setName(jsonArray.getJSONObject(i).optString("name"));
                        poi.setRating(jsonArray.getJSONObject(i).optString("rating", " "));
                        if (jsonArray.getJSONObject(i).has("opening_hours")) {
                            if (jsonArray.getJSONObject(i).getJSONObject("opening_hours").has("open_now")) {
                                if (jsonArray.getJSONObject(i).getJSONObject("opening_hours").getString("open_now").equals("true")) {
                                    poi.setOpenNow("YES");
                                } else {
                                    poi.setOpenNow("NO");
                                }
                            }
                        } else {
                            poi.setOpenNow("Not Known");
                        }
                        if (jsonArray.getJSONObject(i).has("geometry"))
                        {
                            if (jsonArray.getJSONObject(i).getJSONObject("geometry").has("location"))
                            {
                                if (jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").has("lat"))
                                {
                                    poi.setLatLng(Double.parseDouble(jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lat")), Double.parseDouble(jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lng")));
                                }
                            }
                        }
                        if (jsonArray.getJSONObject(i).has("vicinity")) {
                            poi.setVicinity(jsonArray.getJSONObject(i).optString("vicinity"));
                        }
                        if (jsonArray.getJSONObject(i).has("types")) {
                            JSONArray typesArray = jsonArray.getJSONObject(i).getJSONArray("types");
                            for (int j = 0; j < typesArray.length(); j++) {
                                poi.setCategory(typesArray.getString(j) + ", " + poi.getCategory());
                            }
                        }
                    }
                    //if(temp.size()<5)
                    temp.add(poi);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList();
        }
        return temp;
    }
    // View method called for each row of the result
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        ViewHolder holder;
        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.autocomplete, parent, false);
            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.myplaces);
            holder.address=(TextView)convertView.findViewById(R.id.address);
            // Bind the data efficiently with the holder.
            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }
        // If weren't re-ordering this you could rely on what you set last time
        holder.text.setText(Html.fromHtml("<b>"+places.get(position).getName()+"<b>"));
        holder.address.setText(places.get(position).getVicinity());
        return convertView;
    }
    // viewholder class to hold adapter views
    static class ViewHolder {
        TextView text,address;
    }
}