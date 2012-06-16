package com.smdremedy.generator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.smdremedy.generator.KittenGenerator.Kitten;

import pl.smdremedy.generator.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class KittenContactsActivity extends Activity {
    
	private static final String TAG = KittenContactsActivity.class.getSimpleName();
    
	private static final int QUESTION_DIALOG_ID = 0;
	private ProgressBar bar;
	private Button yesNoButton;
	private EditText kittenCountEditText; 
	private int kittenCount = -1;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        kittenCountEditText = (EditText) findViewById(R.id.kitten_count_et);

        yesNoButton = (Button) findViewById(R.id.button1);
        yesNoButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try{
					kittenCount = Integer.parseInt(kittenCountEditText.getText().toString());
					showDialog(QUESTION_DIALOG_ID);
				} catch(NumberFormatException ex) {
					Toast.makeText(KittenContactsActivity.this, R.string.wrong_format, Toast.LENGTH_LONG);
				}
				
			}
		});
        bar = (ProgressBar) findViewById(R.id.progressBar1);
    }
    
    
    class GenerateAsyncTask extends AsyncTask<Integer, Void, String> {

		private static final int FIRST_AVATAR_ID = 100;

		@Override
		protected String doInBackground(Integer... params) {
			Integer contactsCount = params[0];
			for(int i = FIRST_AVATAR_ID; i < FIRST_AVATAR_ID + contactsCount; i++) {
            	addContact(i);
            }
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			bar.setVisibility(View.GONE);
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			bar.setVisibility(View.VISIBLE);
			Toast.makeText(KittenContactsActivity.this, R.string.cute, Toast.LENGTH_LONG);
			super.onPreExecute();
		}
    }
    
    
    @Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case QUESTION_DIALOG_ID:
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			        	if(kittenCount != -1) {
			        		GenerateAsyncTask task = new GenerateAsyncTask();
			        		task.execute(kittenCount);
			        	}
			        	dialog.dismiss();
			        	break;
			        case DialogInterface.BUTTON_NEGATIVE:
			            dialog.dismiss();
			            break;
			        }
			    }
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.sure_question).setPositiveButton(R.string.yes, dialogClickListener)
			    .setNegativeButton(R.string.no, dialogClickListener);
			return builder.create();

		default:
			return super.onCreateDialog(id);
		}
	}



	private void addContact(int i) {
    	
    	Kitten kitty = KittenGenerator.getRandomGuy();
    	
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		int rawContactInsertIndex = ops.size();

		ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());
		
		ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, kitty.name)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, kitty.surname)
                .build());

		ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, kitty.phone) // Number of the person
				.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

		ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, kitty.email) // email of the person
				.withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_MOBILE)
                .build());

		try {
			AssetManager mngr = getAssets();
			byte[] b = getImageForAvatarId(i, mngr);
			
			ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
					.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
					.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
					.withValue(ContactsContract.CommonDataKinds.Photo.DATA15,b) 
					.build());

			ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

            if (res!=null && res.length > 0 && res[0]!=null) {
                Uri newContactUri = res[0].uri;
                Log.d(TAG, "URI added contact:"+ newContactUri);
            } else {
                Log.e(TAG, "Contact not added.");
            }

		} catch (Exception e) {
			// error
			Log.e(TAG, "", e);
		}
	}



	private byte[] getImageForAvatarId(int i, AssetManager mngr)
			throws IOException {
		InputStream is;
		is = mngr.open("avatar_" + Integer.toString(i) + ".jpg");
		
		Bitmap bmImage = BitmapFactory.decodeStream(is);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		bmImage.compress(Bitmap.CompressFormat.JPEG, 80, baos);    
		byte[] b = baos.toByteArray();
		return b;
	}
}