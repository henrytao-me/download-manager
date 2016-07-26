/*
 * Copyright 2016 "Henry Tao <hi@henrytao.me>"
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.henrytao.downloadmanager.sample.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;

import me.henrytao.downloadmanager.sample.ui.base.BaseActivity;
import me.henrytao.firechatengine.sample.R;
import me.henrytao.firechatengine.sample.databinding.ActivityHomeBinding;

/**
 * Created by henrytao on 7/1/16.
 */
public class HomeActivity extends BaseActivity {

  public static Intent newIntent(Activity activity) {
    return new Intent(activity, HomeActivity.class);
  }

  private ActivityHomeBinding mBinding;

  private HomeViewModel mViewModel;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_home, menu);
    return true;
  }

  @Override
  public void onInitializeViewModels() {
    mViewModel = new HomeViewModel();
    addViewModel(mViewModel);
  }

  @Override
  public void onSetContentView(Bundle savedInstanceState) {
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_home);
    mBinding.setViewModel(mViewModel);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setSupportActionBar(mBinding.toolbar);
    requestWriteExternalStoragePermission();
  }

  private void requestWriteExternalStoragePermission() {
    // Should we show an explanation?
    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      new AlertDialog.Builder(this)
          .setTitle("Inform and request")
          .setMessage("You need to enable permissions, bla bla bla")
          .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 300);
            }
          })
          .show();
    } else {
      ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 300);
    }
  }
}
