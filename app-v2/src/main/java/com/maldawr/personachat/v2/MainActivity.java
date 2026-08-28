package com.maldawr.personachat.v2;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(Color.rgb(11, 20, 26));

        TextView title = new TextView(this);
        title.setText("PersonaChat V2");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28f);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Clean rewrite foundation • Simulation");
        sub.setTextColor(Color.rgb(150, 165, 172));
        sub.setTextSize(15f);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 20, 0, 0);
        root.addView(sub);

        setContentView(root);
    }
}
