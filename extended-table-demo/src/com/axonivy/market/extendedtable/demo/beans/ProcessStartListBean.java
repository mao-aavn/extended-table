package com.axonivy.market.extendedtable.demo.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import ch.ivyteam.ivy.application.ActivityState;
import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.app.IApplicationRepository;
import ch.ivyteam.ivy.security.ISecurityContext;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.IProcessStart;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

/**
 * Simple JSF bean to expose Demo process start links for navigation.
 */
@ManagedBean(name = "processStartListBean")
@RequestScoped
public class ProcessStartListBean implements Serializable {

  private static final long serialVersionUID = 1L;

  public static class StartLink {
    private final String name;
    private final String relative;

    StartLink(String name, String relative) {
      this.name = name;
      this.relative = relative;
    }

    public String getName() {
      return name;
    }

    public String getRelative() {
      return relative;
    }
  }

  /**
   * Returns all process starts defined inside process model named "Demo" that
   * point to an .ivp start page. Label is the process start getName().
   */
  public List<StartLink> getDemoProcessStarts() {
    // "Demo" is part of the request path (not necessarily the process model name).
    // Collect all process starts where the full user friendly request path or
    // user friendly request path contains the substring "Demo" (case-insensitive)
    // and expose .ivp links.
    return Sudo.get(() -> {
      List<StartLink> links = new ArrayList<>();
      List<IApplication> apps = IApplicationRepository.instance().allOf(ISecurityContext.current());
      for (IApplication app : apps) {
        for (IProcessModel pm : app.getProcessModelsSortedByName()) {
          IProcessModelVersion pmv = pm.getReleasedProcessModelVersion();
          if (pmv == null || pmv.getActivityState() != ActivityState.ACTIVE) {
            continue;
          }
          IWorkflowProcessModelVersion wf = IWorkflowProcessModelVersion.of(pmv);
          for (IProcessStart ps : wf.getProcessStarts()) {
            try {
              String fullFriendly = null;
              try {
                fullFriendly = ps.getFullUserFriendlyRequestPath();
              } catch (Exception e) {
                // ignore
              }
              String friendly = null;
              try {
                friendly = ps.getUserFriendlyRequestPath();
              } catch (Exception e) {
                // ignore
              }

              String pathToCheck = fullFriendly != null ? fullFriendly : friendly;
              if (pathToCheck == null) {
                continue;
              }

              if (!pathToCheck.toLowerCase().contains("demo")) {
                continue;
              }

              // get link relative and check for .ivp
              String rel = null;
              try {
                if (ps.getLink() != null && ps.getLink().getRelative() != null) {
                  rel = ps.getLink().getRelative();
                }
              } catch (Exception e) {
                // ignore
              }

              if (rel == null) {
                // fallback to full request path
                try {
                  rel = ps.getFullUserFriendlyRequestPath();
                } catch (Exception e) {
                  // ignore
                }
              }

              if (rel != null && rel.endsWith(".ivp")) {
                links.add(new StartLink(ps.getName(), rel+ "?embedInFrame"));
              }
            } catch (Exception e) {
              // ignore inaccessible starts
            }
          }
        }
      }
      return links;
    });
  }

}
