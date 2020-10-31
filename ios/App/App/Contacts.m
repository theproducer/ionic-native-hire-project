#import <Capacitor/Capacitor.h>

CAP_PLUGIN(Contacts, "Contacts",
  CAP_PLUGIN_METHOD(getAll, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(getById, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(getBySearch, CAPPluginReturnPromise);
)
