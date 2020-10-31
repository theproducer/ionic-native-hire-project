import Foundation
import Contacts
import Capacitor

enum ContactError: Error {
    case accessDenied
    case genericError(errorMessage: String)
    case validationError(errorMessage: String)
}

extension ContactError: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .validationError(let msg):
            return "Validation Error: \(msg)"
        case .accessDenied:
            return "Access to contacts has not been granted"
        case .genericError(let msg):
            return "Unknown Error: \(msg)"
        }
    }
}

struct AppContact: Codable {
    var id: String
    var firstName: String
    var lastName: String
    var emailAddresses: [String] = []
    var phoneNumbers: [String] = []
}

@objc(Contacts)
public class Contacts : CAPPlugin {
    private var store: CNContactStore?
    private var access: Bool = false
    
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
    
    @objc func getAll(_ call: CAPPluginCall) {
        getAllContacts(search: nil, byGroup: nil) { (result) in
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
    
    @objc func getById(_ call: CAPPluginCall) {
        guard let contactId = call.getString("contactId") else {
            call.reject(ContactError.validationError(errorMessage: "a contact id is required").localizedDescription)
            return
        }
        
        getContact(contactId: contactId) { (result) in
            switch result {
            case .failure(let err):
                call.reject(err.localizedDescription, nil, err)
            case .success(let contact):
                if let contact = contact {
                    var contactDict: [String:Any] = [:]
                    contactDict["id"] = contact.id
                    contactDict["firstName"] = contact.firstName
                    contactDict["lastName"] = contact.lastName
                    contactDict["phoneNumbers"] = contact.phoneNumbers
                    contactDict["emailAddresses"] = contact.emailAddresses
                    
                    call.resolve([
                        "contact": contactDict
                    ])
                } else {
                    call.resolve(["contact": NSNull()])
                }
            }
        }
    }
    
    @objc func getBySearch(_ call: CAPPluginCall) {
        guard let query = call.getString("searchTerm") else {
            call.reject(ContactError.validationError(errorMessage: "a searchTerm is required to search contacts").localizedDescription)
            return
        }
        
        let group = call.getString("groupName")
        
        getAllContacts(search: query, byGroup: group) { (result) in
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

    
    private func appContactsToDict(contacts: [AppContact]) -> [[String: Any]] {
        var returnDict: [[String: Any]] = []
        
        for (_, contact) in contacts.enumerated() {
            var contactDict: [String:Any] = [:]
            contactDict["id"] = contact.id
            contactDict["firstName"] = contact.firstName
            contactDict["lastName"] = contact.lastName
            contactDict["phoneNumbers"] = contact.phoneNumbers
            contactDict["emailAddresses"] = contact.emailAddresses
            
            returnDict.append(contactDict)
        }
        
        return returnDict
    }
    
    private func buildAppContact(contact: CNContact) -> AppContact {
        var appcontact = AppContact(id: contact.identifier ,firstName: contact.givenName, lastName: contact.familyName)
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
        
        return appcontact
    }
    
    private func searchEmailAddresses(search: String, emails: [CNLabeledValue<NSString>]) -> Bool {
        for (_, email) in emails.enumerated() {
            if email.value.contains(search) {
                return true
            }
        }
        
        return false
    }
    
    private func searchPhoneNumners(search: String, numbers: [CNLabeledValue<CNPhoneNumber>]) -> Bool {
        for (_, phone) in numbers.enumerated() {
            if phone.value.stringValue.contains(search) {
                return true
            }
        }
        
        return false
    }
    
    private func getContact(contactId: String, completion: @escaping (_ result: Result<AppContact?, Error>) -> Void) {
        guard let store = self.store else {
            completion(.failure(ContactError.genericError(errorMessage: "store is nil")))
            return
        }
        
        if !access {
            completion(.failure(ContactError.accessDenied))
            return
        }
        
        let keys = [CNContactFamilyNameKey, CNContactGivenNameKey, CNContactPhoneNumbersKey, CNContactEmailAddressesKey, CNContactIdentifierKey] as [CNKeyDescriptor]
        let contactsRequest = CNContactFetchRequest(keysToFetch: keys)
        contactsRequest.predicate = CNContact.predicateForContacts(withIdentifiers: [contactId])
        
        DispatchQueue.global(qos: .background).async {
            do {
                try store.enumerateContacts(with: contactsRequest, usingBlock: { (contact, _) in
                    let appcontact = self.buildAppContact(contact: contact)
                    completion(.success(appcontact))
                })
                
                DispatchQueue.main.async {
                    completion(.success(nil))
                }
            } catch let err {
                DispatchQueue.main.async {
                    completion(.failure(err))
                }
            }
        }
    }
    
    private func getAllContacts(search: String?, byGroup: String?, completion: @escaping (_ result: Result<[AppContact], Error>) -> Void) {
        guard let store = self.store else {
            completion(.failure(ContactError.genericError(errorMessage: "store is nil")))
            return
        }
        
        if !access {
            completion(.failure(ContactError.accessDenied))
            return
        }
        
        let keys = [CNContactFamilyNameKey, CNContactGivenNameKey, CNContactPhoneNumbersKey, CNContactEmailAddressesKey,CNContactIdentifierKey] as [CNKeyDescriptor]
        
        let contactsRequest = CNContactFetchRequest(keysToFetch: keys)
        contactsRequest.unifyResults = true
        
        DispatchQueue.global(qos: .background).async {
            do {
                if let groupName = byGroup {
                    var foundGroup: CNGroup? = nil
                    // try to find a group by this name
                    for (_, group) in try store.groups(matching: nil).enumerated() {
                        if group.name == groupName {
                            foundGroup = group
                            break
                        }
                    }
                    
                    if let foundGroup = foundGroup {
                        contactsRequest.predicate = CNContact.predicateForContactsInGroup(withIdentifier: foundGroup.identifier)
                    }
                }
                
                var allContacts: [AppContact] = []
                
                try store.enumerateContacts(with: contactsRequest, usingBlock: { (contact, _) in
                    if let search = search {
                        var found = false
                        // search for contacts if name, phone, or  email contains part of the search term
                        if contact.givenName.contains(search) || contact.familyName.contains(search) {
                            found = true
                        }
                        
                        if !found {
                            // if not already found by name, look through the email and phone arrays
                            if self.searchPhoneNumners(search: search, numbers: contact.phoneNumbers) || self.searchEmailAddresses(search: search, emails: contact.emailAddresses) {
                                found = true
                            }
                        }
                        
                        if found {
                            let appcontact = self.buildAppContact(contact: contact)
                            allContacts.append(appcontact)
                        }
                    } else {
                        let appcontact = self.buildAppContact(contact: contact)
                        allContacts.append(appcontact)
                    }
                    
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
