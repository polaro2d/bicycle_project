package yu.Nmap;

import android.app.Activity;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;

public class SplashActivity extends Activity {
    private final int SPLASH_DISPLAY_LENGTH = 3000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

         /* SPLASH_DISPLAY_LENGTH 뒤에 메뉴 액티비티를 실행시키고 종료한다.*/

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                // 메뉴액티비티를 실행하고 로딩화면을 죽인다.
                Intent mainIntent = new Intent(SplashActivity.this, NMapViewer.class);
                startActivity(mainIntent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
       /* Handler hd = new Handler();

        hd.postDelayed(new Runnable() {

            public void run() {

                finish();            //2초동안 보여준 후, SplashActivity를 종료한 후 MainActivity로 돌아갑니다.

            }

        }, 3000);                //2000millis. 즉 2초동안 해당 화면을 보여준 후, finish() 됩니다.
        */
    }
}

