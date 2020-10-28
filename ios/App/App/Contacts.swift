import Foundation
import Contacts
import Capacitor

enum ContactError: Error {
    case accessDenied
    case genericError(errorMessage: String)
}

extension ContactError: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .accessDenied:
            return "Access to contacts has not been granted"
        case .genericError(let msg):
            return "Unknown Error: \(msg)"
        }
    }
}

struct AppContact: Codable {
    var firstName: String
    var lastName: String
    var emailAddresses: [String] = []
    var phoneNumbers: [String] = []
}

@objc(Contacts)
public class Contacts : CAPPlugin {
    private var store: CNContactStore?
    private var access: Bool = false
    
    @objc func getAll(_ call: CAPPluginCall) {
        getAllContacts { (result) in
            switch result {
            case .failure(let err):
                call.reject(err.localizedDescription, nil, err)
            case .success(let contacts):
                print(contacts)
                let contactsDict = self.appContactsToDict(contacts: contacts)
                call.resolve([
                    "contacts": contactsDict
                ])
            }
        }
    }
    
    @objc override public func load() {
        if store == nil {
            self.store = CNContactStore()
        }
        
        if let store = self.store {
            store.requestAccess(for: .contacts) { (granted, err) in
                self.access = granted
            }
        }
    }
    
    private func getAllMocked() -> [Any] {
        return [
            [
                "firstName": "Elton",
                "lastName": "Json",
                "phoneNumbers": ["2135551111"],
                "emailAddresses": ["elton@eltonjohn.com"],
            ],
            [
                "firstName": "Freddie",
                "lastName": "Mercury",
                "phoneNumbers": [],
                "emailAddresses": [],
            ],
        ]
    }
    
    private func appContactsToDict(contacts: [AppContact]) -> [[String: Any]] {
        var returnDict: [[String: Any]] = []
        
        for (_, contact) in contacts.enumerated() {
            var contactDict: [String:Any] = [:]
            contactDict["firstName"] = contact.firstName
            contactDict["lastName"] = contact.lastName
            contactDict["phoneNumbers"] = contact.phoneNumbers
            contactDict["emailAddresses"] = contact.emailAddresses
            
            returnDict.append(contactDict)
        }
        
        return returnDict
    }
    
    private func getAllContacts(completion: @escaping (_ result: Result<[AppContact], Error>) -> Void) {
        guard let store = self.store else {
            completion(.failure(ContactError.genericError(errorMessage: "store is nil")))
            return
        }
        
        if !access {
            completion(.failure(ContactError.accessDenied))
            return
        }
                
        let keys = [CNContactFamilyNameKey, CNContactGivenNameKey, CNContactPhoneNumbersKey, CNContactEmailAddressesKey] as [CNKeyDescriptor]
        let contactsRequest = CNContactFetchRequest(keysToFetch: keys)
        
        DispatchQueue.global(qos: .background).async {
            do {
                var allContacts: [AppContact] = []
                
                try store.enumerateContacts(with: contactsRequest, usingBlock: { (contact, _) in
                    var appcontact = AppContact(firstName: contact.givenName, lastName: contact.familyName)
                    for (_, email) in contact.emailAddresses.enumerated() {
                        if email.value != "" {
                            appcontact.emailAddresses.append(email.value as String)
                        }
                    }
                    
                    for (_, phone) in contact.phoneNumbers.enumerated() {
                        if phone.value.stringValue != "" {
                            appcontact.phoneNumbers.append(phone.value.stringValue)
                        }
                    }
                    
                    allContacts.append(appcontact)
                })
                
                DispatchQueue.main.async {
                    completion(.success(allContacts))
                }
            } catch let err {
                DispatchQueue.main.async {
                    completion(.failure(err))
                }
            }
        }
    }
}
