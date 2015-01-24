package getdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by dhruv on 6/12/14.
 * This class takes the response text as an input from
 * the AsyncTask class and returns a list of recruiters
 * found in the response text of the page
 *
 * HashMap<String, String> for each recruiter has the
 * following structure :-
 *      url -       url of the recruiter
 *      category -  one of the S, A+, A, B
 *      accepting - if the recruiter is accepting applications
 *      name -      name of the recruiter
 *      branches -  eligible branches
 *      BE -        Yes if open for BE, No if Not
 *      ME -        Yes if open for ME, No if Not
 *      Intern -    Yes if open for Intern, No if Not
 *      MBA -       Yes if open for MBA, No if Not
 *      visitDate - date of visit
 *
 */
public class GetRecruiterData {

    private String responseText;
    private int baseLineNumber = 131;

    public GetRecruiterData(String responseText) {
        this.responseText = responseText;
    }

    public List<HashMap<String, String>> getDataList() {
        List<HashMap<String, String>> recruitersList = new ArrayList<>();
        String[] lines = responseText.split("\n");
        int i = 0;
        try {
            while (i < 15) {
                HashMap<String, String> recruiter = new HashMap<>();

                //  for fetching the recruiter URL
                int pathIndex = lines[baseLineNumber].indexOf("recruiter_detail");
                int pathIndexEnd = lines[baseLineNumber].indexOf("');");
                String recruiterUrl = lines[baseLineNumber].substring(pathIndex, pathIndexEnd);
                recruiterUrl = recruiterUrl.replaceAll(" ", "%20");
                String placementUrl = "http://www.dce.ac.in/placement/";
                recruiterUrl = placementUrl + recruiterUrl;
                recruiter.put("url", recruiterUrl);

                //  for fetching the category
                int categoryIndex = lines[baseLineNumber + 1].indexOf("style1") + 8;
                int categoryIndexEnd = lines[baseLineNumber + 1].indexOf("</td>");
                String category = lines[baseLineNumber + 1].substring(categoryIndex, categoryIndexEnd);
                recruiter.put("category", category);

                //  for fetching the acceptance status
                String acceptanceStatus;
                if (lines[baseLineNumber + 2].contains("Y_led"))
                    acceptanceStatus = "Yes";
                else
                    acceptanceStatus = "No";
                recruiter.put("accepting", acceptanceStatus);

                //  for fetching the name of the company
                int nameIndex = lines[baseLineNumber + 3].indexOf("<td>") + 4;
                int nameIndexEnd = lines[baseLineNumber + 3].indexOf("</td>");
                String recruiterName = lines[baseLineNumber + 3].substring(nameIndex, nameIndexEnd);
                recruiter.put("name", recruiterName);

                //  for fetching eligible branches
                int branchIndex = lines[baseLineNumber + 4].indexOf("<td>") + 4;
                int branchIndexEnd = lines[baseLineNumber + 4].indexOf("</td>");
                String branches = lines[baseLineNumber + 4].substring(branchIndex, branchIndexEnd);
                recruiter.put("branches", branches);

                //  to check if open for BE
                if (lines[baseLineNumber + 5].contains("images/Y"))
                    recruiter.put("BE", "Yes");
                else
                    recruiter.put("BE", "No");

                //  to check if open for ME
                if (lines[baseLineNumber + 6].contains("images/Y"))
                    recruiter.put("ME", "Yes");
                else
                    recruiter.put("ME", "No");

                //  to check if open for Intern
                if (lines[baseLineNumber + 7].contains("images/Y"))
                    recruiter.put("Intern", "Yes");
                else
                    recruiter.put("Intern", "No");

                //  to check if open for MBA
                if (lines[baseLineNumber + 8].contains("images/Y"))
                    recruiter.put("MBA", "Yes");
                else
                    recruiter.put("MBA", "No");

                //  for fetching the date of visit
                int dateIndex = lines[baseLineNumber + 9].indexOf("<td>") + 4;
                int dateIndexEnd = lines[baseLineNumber + 9].indexOf("</td>");
                String visitDate = lines[baseLineNumber + 9].substring(dateIndex, dateIndexEnd);
                recruiter.put("visitDate", visitDate);
                recruitersList.add(recruiter);

                baseLineNumber += 11;
                i++;
            }
        }
        catch (StringIndexOutOfBoundsException e) {
            return recruitersList;
        }
        return recruitersList;
    }
}