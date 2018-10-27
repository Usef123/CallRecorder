package net.synapticweb.callrecorder.contactdetail;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import net.synapticweb.callrecorder.AppLibrary;
import net.synapticweb.callrecorder.PhoneTypeContainer;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.data.Contact;

import java.io.File;
import java.io.IOException;


public class EditContactActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Contact contact;
    private ImageView contactPhoto;
    private EditText contactName;
    private EditText contactPhone;
    private Spinner phoneType;
    private boolean dataChanged = false;
    private boolean setInitialPhoneType = false;
    private File savedPhotoPath = null;
    private Uri oldPhotoUri = null;
    private static final String TAG = "CallRecorder";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int TAKE_PICTURE = 2;
    public static final String EDITED_CONTACT = "edited_contact";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("contact", contact);
        outState.putBoolean("dataChanged", dataChanged);
        outState.putBoolean("setInitialPhoneType", setInitialPhoneType);
        outState.putSerializable("savedPhotoPath", savedPhotoPath);
        outState.putParcelable("oldPhotoUri", oldPhotoUri);
    }

    private void onCancelOrBackPressed() {
        if (dataChanged) {
            new MaterialDialog.Builder(EditContactActivity.this)
                    .title(R.string.discard_edit_title)
                    .icon(getResources().getDrawable(R.drawable.warning))
                    .content(R.string.discard_edit_message)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if(savedPhotoPath != null) {
                                getContentResolver().delete(contact.getPhotoUri(), null, null);
                            }
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    })
                    .show();
        }
        else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_contact_activity);

        if(savedInstanceState != null) {
            contact = savedInstanceState.getParcelable("contact");
            dataChanged = savedInstanceState.getBoolean("dataChanged");
            setInitialPhoneType = savedInstanceState.getBoolean("setInitialPhoneType");
            savedPhotoPath = (File) savedInstanceState.getSerializable("savedPhotoPath");
            oldPhotoUri = savedInstanceState.getParcelable("oldPhotoUri");
        }
        else
            contact = getIntent().getExtras().getParcelable(ContactDetailPresenter.EDIT_EXTRA_CONTACT);

        Button cancelButton = findViewById(R.id.edit_phone_number_cancel);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelOrBackPressed();
            }
        });

        Button okButton = findViewById(R.id.edit_phone_number_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dataChanged) {
//                    Cursor cursor = (Cursor) phoneType.getSelectedItem(); //https://stackoverflow.com/questions/5787809/get-spinner-selected-items-text
                    contact.setUnkownNumber(false);
                    if(oldPhotoUri != null) { //a fost selectată altă poză din galerie sau a fost luată altă poză
                        // cu camera sau a fost scoasă poza existentă.
                        //întotdeauna este poza noastră!
                            getContentResolver().delete(oldPhotoUri, null, null);
                    }

                    contact.updateContact(EditContactActivity.this, false);
                    Intent intent = new Intent();
                    intent.putExtra(EDITED_CONTACT, contact);
                    setResult(RESULT_OK, intent);
                }
                else
                    setResult(RESULT_CANCELED);
                finish();
            }
        });

        contactPhoto = findViewById(R.id.edit_phone_number_photo);

        if(contact.getPhotoUri() != null)
            contactPhoto.setImageURI(contact.getPhotoUri());
        else {
            if(contact.isPrivateNumber())
                contactPhoto.setImageResource(R.drawable.user_contact_yellow);
            else if(contact.isUnkownNumber())
                contactPhoto.setImageResource(R.drawable.user_contact_red);
            else
                contactPhoto.setImageResource(R.drawable.user_contact_blue);
        }

        registerForContextMenu(contactPhoto);
        contactPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.showContextMenu();
            }
        });

        contactName = findViewById(R.id.edit_name);
        contactName.setText(contact.getContactName(), TextView.BufferType.EDITABLE);
        contactName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(!contactName.getText().toString().equals(contact.getContactName())) {
                    contact.setContactName(contactName.getText().toString());
                    dataChanged = true;
                }
            }
        });

        contactPhone = findViewById(R.id.edit_number);
        contactPhone.setText(contact.getPhoneNumber(), TextView.BufferType.EDITABLE);
        contactPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(!contactPhone.getText().toString().equals(contact.getPhoneNumber())) {
                    contact.setPhoneNumber(contactPhone.getText().toString());
                    dataChanged = true;
                }
            }
        });

        //Pentru aceeași chestie cu baze de date: https://stackoverflow.com/questions/13413030/using-simplecursoradapter-with-spinner
        phoneType = findViewById(R.id.edit_types);
        phoneType.setOnItemSelectedListener(this);
        ArrayAdapter<PhoneTypeContainer> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, AppLibrary.PHONE_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        phoneType.setAdapter(adapter);

        int position;
        for(position = 0; position < AppLibrary.PHONE_TYPES.size(); ++position)
            if(AppLibrary.PHONE_TYPES.get(position).getTypeCode() == contact.getPhoneTypeCode())
                break;
        //https://stackoverflow.com/questions/11072576/set-selected-item-of-spinner-programmatically
        phoneType.setSelection(adapter.getPosition(AppLibrary.PHONE_TYPES.get(position)));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_contact_change_photo, menu);

        //pentru micșorarea fontului: https://stackoverflow.com/questions/29844064/how-to-change-the-menu-text-size
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            int end = spanString.length();
            spanString.setSpan(new RelativeSizeSpan(0.87f), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(spanString);
        }

        MenuItem menuItem = menu.getItem(0);
        if(contact.getPhotoUri() == null)
            menuItem.setEnabled(false);
        menuItem = menu.getItem(2);
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            menuItem.setEnabled(false);
    }

    //timestampul este necesar pentru situația cînd se schimbă pozele și trebuie ștearsă cea veche.
    private void setPhotoPath() {
        savedPhotoPath = new File(getFilesDir(), contact.getPhoneNumber() + System.currentTimeMillis() + ".jpg");
    }

    //http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
    private void selectPhoto() {
        setPhotoPath();
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void removePhoto() {
        oldPhotoUri = contact.getPhotoUri();
        contact.setPhotoUri((Uri) null); //ambigous method call
        contactPhoto.setImageResource(R.drawable.user_contact_blue);
        dataChanged = true;
    }

    private void takePhoto(){
        setPhotoPath();
        try {
            savedPhotoPath.createNewFile();
        }
        catch (IOException ioe) {
            Log.wtf(TAG, ioe.getMessage());
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, "net.synapticweb.callrecorder.fileprovider", savedPhotoPath));
        //fără chestia de mai jos aplicația foto crapă în kitkat cu java.lang.SecurityException:
        // Permission Denial: opening provider android.support.v4.content.FileProvider
        //https://stackoverflow.com/questions/24467696/android-file-provider-permission-denial
        if ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ) {
            intent.setClipData(ClipData.newRawUri("", FileProvider.getUriForFile(this, "net.synapticweb.callrecorder.fileprovider", savedPhotoPath)));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri chosenPhotoUri;
        if (resultCode != Activity.RESULT_OK ) {
            Log.wtf(TAG, "The result code is error");
            if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                Exception error = result.getError();
                Log.wtf(TAG, error.getMessage());
            }
            return;
        }

        if (requestCode == PICK_IMAGE_REQUEST && (chosenPhotoUri = data.getData()) != null) {
            CropImage.activity(chosenPhotoUri).setCropShape(CropImageView.CropShape.OVAL)
                    .setOutputUri(FileProvider.getUriForFile(this, "net.synapticweb.callrecorder.fileprovider", savedPhotoPath))
                    .setAspectRatio(1,1)
                    .setMaxCropResultSize(2000, 2000) //vezi mai jos comentariul
                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setOutputCompressQuality(70)
                    .start(this);
        }
        else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            chosenPhotoUri = result.getUri();
            contactPhoto.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
            contactPhoto.setImageURI(chosenPhotoUri);
            this.oldPhotoUri = contact.getPhotoUri();
            contact.setPhotoUri(chosenPhotoUri);
            dataChanged = true;
        }
        else if(requestCode == TAKE_PICTURE) {
            CropImage.activity(FileProvider.getUriForFile(this, "net.synapticweb.callrecorder.fileprovider", savedPhotoPath))
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setOutputUri(FileProvider.getUriForFile(this, "net.synapticweb.callrecorder.fileprovider", savedPhotoPath))
                    .setMaxCropResultSize(2000, 2000) //necesar, pentru că dacă poza e prea mare apare un rotund negru
                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG) //necesar, pentru că fișierul output are
                    //totdeauna extensia .jpg
                    .setOutputCompressQuality(70)
                    .start(this);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        if(!setInitialPhoneType) {
            setInitialPhoneType = true;
            return ;
        }
        if(!phoneType.getSelectedItem().toString().equals(contact.getPhoneTypeName())) {
            contact.setPhoneType(phoneType.getSelectedItem().toString());
            dataChanged = true;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onBackPressed() {
        onCancelOrBackPressed();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_photo_remove:
                removePhoto();
                return true;
            case R.id.change_photo_select:
                selectPhoto();
                return true;
            case R.id.change_photo_takenew:
                takePhoto();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}