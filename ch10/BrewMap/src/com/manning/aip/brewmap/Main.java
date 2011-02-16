package com.manning.aip.brewmap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.manning.aip.brewmap.model.Pub;
import com.manning.aip.brewmap.xml.BeerMappingParser;
import com.manning.aip.brewmap.xml.BeerMappingXmlPullParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// NOTE -- this is far from complete, just an interim checkin

public class Main extends Activity {

   private static final String CITY = "CITY";
   private static final String STATE = "STATE";
   private static final String PIECE = "PIECE";

   private ProgressDialog progressDialog;

   private Geocoder geocoder;

   private BeerMappingParser parser;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      progressDialog = new ProgressDialog(this);
      progressDialog.setCancelable(false);
      progressDialog.setMessage("Retrieving data...");

      geocoder = new Geocoder(this);
      // note that API level 9 added the "isPresent" method which could be checked here

      parser = new BeerMappingXmlPullParser();

      final EditText input = (EditText) findViewById(R.id.input);

      Button city = (Button) findViewById(R.id.button_city);
      city.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            new ParseFeedTask().execute(new String[] { CITY, input.getText().toString() });
         }
      });

      Button state = (Button) findViewById(R.id.button_state);
      state.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            new ParseFeedTask().execute(new String[] { STATE, input.getText().toString() });
         }
      });

      Button piece = (Button) findViewById(R.id.button_piece);
      piece.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            new ParseFeedTask().execute(new String[] { PIECE, input.getText().toString() });
         }
      });
   }

   private void forwardResults(List<Pub> pubs) {
      if (pubs != null && !pubs.isEmpty()) {
         StringBuilder sb = new StringBuilder();
         for (Pub p : pubs) {
            sb.append("\n\n" + p);
         }
         Intent i = new Intent(this, QueryResults.class);
         i.putExtra("RESULTS", sb.toString());
         startActivity(i);
      } else {
         Toast.makeText(this, "Pubs empty!", Toast.LENGTH_SHORT).show();
      }
   }

   private class ParseFeedTask extends AsyncTask<String, Void, List<Pub>> {

      @Override
      protected void onPreExecute() {
         progressDialog.show();
      }

      @Override
      protected List<Pub> doInBackground(String... args) {
         List<Pub> result = new ArrayList<Pub>();
         if (args == null || args.length != 2) {
            return result;
         }
         String type = args[0];
         String input = args[1];
         if (type.equals(CITY)) {
            result = parser.parseCity(input);
         } else if (type.equals(STATE)) {
            result = parser.parseState(input);
         } else if (type.equals(PIECE)) {
            result = parser.parsePiece(input);
         }

         // geocode the city/state/zip form addresses in the task too
         for (Pub p : result) {
            try {
               List<android.location.Address> addresses =
                        geocoder.getFromLocationName(p.getAddress().getLocationName(), 1);
               if (addresses != null && !addresses.isEmpty()) {
                  android.location.Address a = addresses.get(0);
                  p.setLatitude(a.getLatitude());
                  p.setLongitude(a.getLongitude());
               }
            } catch (IOException e) {
               Log.e(Constants.LOG_TAG, "Error geocoding location name", e);
            }
         }

         return result;
      }

      @Override
      protected void onPostExecute(List<Pub> pubs) {
         if (progressDialog.isShowing()) {
            progressDialog.hide();
         }
         forwardResults(pubs);
      }
   }
}