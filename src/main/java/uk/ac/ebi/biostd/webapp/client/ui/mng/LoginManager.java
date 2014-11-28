package uk.ac.ebi.biostd.webapp.client.ui.mng;

import uk.ac.ebi.biostd.authz.User;

public interface LoginManager
{
 void askForLogin();
 
 void userLoggedIn( User u );
}
