package com.rendergram;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Main fragment
 */
public class MainFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int SELECT_PHOTO = 2;
    private static final String TAG = "MainFragment";

    private RenderScript mRenderScript;
    private ImageView mImageView;
    private final View.OnClickListener mOnSubmitListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.submit:
                            submitInvert();
                            break;
                        default:
                            break;
                    }
                }
            };
    private ScriptC_invert mScriptInvert;
    private Bitmap mImageBitmap;

    private void submitInvert() {
        setupRenderScriptIfNeeded();
        Bitmap result = Bitmap.createBitmap(mImageBitmap);
        Allocation in = Allocation.createFromBitmap(mRenderScript, mImageBitmap);
        Allocation out = Allocation.createFromBitmap(mRenderScript, result);
        mScriptInvert.forEach_invert(in, out);
        out.copyTo(result);
        mImageView.setImageBitmap(result);
    }

    public MainFragment() {
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mImageView = (ImageView) rootView.findViewById(R.id.imageView);
        rootView.findViewById(R.id.submit).setOnClickListener(mOnSubmitListener);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == Activity.RESULT_OK) {
                    Bundle extras = data.getExtras();
                    mImageBitmap = (Bitmap) extras.get("data");
                    mImageView.setImageBitmap(mImageBitmap);
                    return;
                } else {
                    showCantOpenToast();
                }
                break;

            case SELECT_PHOTO:
                if (resultCode ==  Activity.RESULT_OK) {
                    Uri selectedImage = data.getData();
                    try {
                        mImageBitmap = decodeUri(selectedImage);
                    } catch (FileNotFoundException e) {
                        Log.i(TAG, "Catch error: ", e);
                        showCantOpenToast();
                    }
                    mImageView.setImageBitmap(mImageBitmap);
                    return;
                } else {
                    showCantOpenToast();
                }
                break;
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 140;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }

    private ContentResolver getContentResolver() {
        return getActivity().getContentResolver();
    }

    private void showCantOpenToast() {
        Toast.makeText(getActivity(), "Can't open photo :(", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.action_camera:
                dispatchTakePictureIntent();
                return true;

            case R.id.action_choose_photo:
                choosePhoto();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void choosePhoto() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }


    private void setupRenderScriptIfNeeded() {
        if (mRenderScript == null) {
            mRenderScript = RenderScript.create(getActivity());
        }
        if (mScriptInvert == null){
            mScriptInvert = new ScriptC_invert(mRenderScript);
        }

    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
}
