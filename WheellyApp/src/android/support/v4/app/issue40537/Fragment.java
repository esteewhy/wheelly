package android.support.v4.app.issue40537;

import java.util.List;
import android.content.Intent;

/**
 * Fixes onActivityResult not propagating to nested fragments.
 * 
 * https://code.google.com/p/android/issues/detail?id=40537
 */
public class Fragment extends android.support.v4.app.Fragment {
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
        List<android.support.v4.app.Fragment> fragments = getChildFragmentManager().getFragments();
        if (fragments != null) {
            for (android.support.v4.app.Fragment fragment : fragments) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
	}
}