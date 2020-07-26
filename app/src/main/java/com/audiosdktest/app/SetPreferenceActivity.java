package com.audiosdktest.app;

import android.app.Activity;
import android.os.Bundle;
import com.juked.app.R;

public class SetPreferenceActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();



	}



}
