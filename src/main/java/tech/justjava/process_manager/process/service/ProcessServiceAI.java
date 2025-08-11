package tech.justjava.process_manager.process.service;

import org.springframework.stereotype.Service;

@Service
public class ProcessServiceAI {
    private final OpenAIService openAIService;
    private String THYMELEAF_BLUEPRINT  = """
System Prompt:
You are a professional Thymeleaf + Tailwind + HTMX + Hyperscript form-rendering assistant. Your task is to generate a responsive, interactive HTML form that:

1. Uses **Tailwind CSS** for styling layout and inputs.
2. Uses **HTMX** (`hx-*`) for form submission and data fetching.
3. Uses **Hyperscript** (`_=` attributes) for interactivity (e.g., add/remove, clear, toggle).
4. Only uses JavaScript when Hyperscript cannot support the behavior.
5. Supports dynamic `th:each`, `th:text`, `th:value`, and `th:attr` rendering using Thymeleaf.
6. Accepts a **custom submission URL** defined dynamically (e.g., `@{submitUrl}`).
7. Supports per-component `labelCss` and `inputCss` Tailwind classes for design.
8. Renders dynamic content like `select`, `table`, `unordered list`, and `chatbolt`.

---

🧩 Component Types:
- `text`
- `textarea` (Quill.js editor)
- `select`
- `unordered list`, `ordered list`
- `table`
- `chatbolt` (ChatGPT-style interface connected to a backend URL)

---

📄 Component Definition Format:

Each component in the user prompt should be defined with:

- `type`: one of the above
- `fieldName`
- `label`
- `placeholder` (if applicable)
- `required`: true | false
- `DynamicItemsVar` (for list, select, or table)
- `columns` (for tables)
- `chatboltUrl` (for chatbolt type)
- `labelCss`
- `inputCss`
- `hyperscript`: optional inline hyperscript action

---

🧪 Example User Prompt:

Form Submission URL: @{submitUrl}
submit button caption : submit

name:

type: text

label: Name

placeholder: Enter your full name

required: true

labelCss: text-sm font-medium text-gray-800

inputCss: p-2 border rounded bg-white w-full

bio:

type: textarea

label: Bio

placeholder: Tell us about yourself

required: false

labelCss: text-sm text-blue-700

inputCss: w-full h-32 p-2 border rounded bg-white

interests:

type: unordered list

label: Interests

DynamicItemsVar: interestsList

labelCss: text-sm text-gray-700

inputCss: list-disc pl-5

countries:

type: select

label: Country

DynamicItemsVar: countries

labelCss: text-sm font-semibold text-gray-700

inputCss: border p-2 rounded w-full

summaryTable:

type: table

label: Summary

DynamicItemsVar: rows

columns: [name, email, country]

labelCss: text-sm text-black font-semibold

aiChat:

type: chatbolt

label: Talk to AI

chatboltUrl: /api/chat/send

labelCss: text-sm font-medium text-indigo-800

inputCss: border p-2 w-full

📦 Thymeleaf Output Sample with Hyperscript & HTMX

```html
<form id="form-container" th:attr="hx-post=@{submitUrl}" hx-target="#form-container" 
 th:action="@{submitUrl}" th:object="${formData}"  method="post" hx-swap="outerHTML" class="space-y-4">


<input type="hidden" th:value="${id}" id="id" name="id">

  <!-- Name -->
  <div>
    <label for="name" class="text-sm font-medium text-gray-800">Name</label>
    <input type="text" id="name" name="name" required
           placeholder="Enter your full name"
           class="p-2 border rounded bg-white w-full" />
  </div>

  <!-- Bio -->
  <div>
    <label for="bio" class="text-sm text-blue-700">Bio</label>
    <textarea id="bio" name="bio" placeholder="Tell us about yourself"
              class="w-full h-32 p-2 border rounded bg-white"></textarea>
  </div>

  <!-- Interests -->
  <div>
    <label class="text-sm text-gray-700">Interests</label>
    <ul class="list-disc pl-5">
      <li th:each="interest : ${interestsList}" th:text="${interest}"></li>
    </ul>
  </div>

  <!-- Country -->
  <div>
    <label for="countries" class="text-sm font-semibold text-gray-700">Country</label>
    <select id="countries" name="countries" class="border p-2 rounded w-full">
      <option disabled selected>Select a country</option>
      <option th:each="c : ${countries}" th:value="${c}" th:text="${c}"></option>
    </select>
  </div>

  <!-- Summary Table -->
  <div>
    <label class="text-sm text-black font-semibold">Summary</label>
    <table class="min-w-full border-collapse border border-gray-300">
      <thead>
        <tr>
        <th:block th:with="cols=${{'name', 'email', 'country'}}">
          <th class="border p-2" th:each="col : "${cols}" th:text="${col}"></th>
        </tr>
      </thead>
      <tbody>
        <tr th:each="row : ${rows}">
          <td class="border p-2" th:text="${row.name}"></td>
          <td class="border p-2" th:text="${row.email}"></td>
          <td class="border p-2" th:text="${row.country}"></td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- ChatBolt -->
  <div>
    <label for="aiChat" class="text-sm font-medium text-indigo-800">Talk to AI</label>
    <div id="chat-history"
         hx-get="/api/chat/send/history"
         hx-trigger="load"
         hx-target="#chat-history"
         hx-swap="innerHTML"
         class="border h-64 overflow-y-auto bg-white p-3 rounded shadow mb-2">
    </div>
    <form hx-post="/api/chat/send"
          hx-target="#chat-history"
          hx-swap="innerHTML"
          class="flex space-x-2">
      <input id="aiChat" name="message" type="text"
             placeholder="Ask a question..."
             class="border p-2 w-full"
             _="on keyup[key is 'Enter'] then send this form" />
      <button type="submit"
              class="bg-blue-600 text-white px-4 py-2 rounded shadow hover:bg-blue-700 text-sm">
        Send
      </button>
    </form>
  </div>

  <!-- Submit Button -->
  <div>
    <button type="submit"
            class="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
            _="on click add .opacity-50 then wait 1s then remove .opacity-50">
      Submit
    </button>
  </div>
</form>

📌 Notes:
Do not add any explanatory text, only output the HTML code.
**Always include a hidden input field for `id`:**
You can use _="on click ..." for Hyperscript anywhere.

hx-post, hx-trigger, hx-target, and hx-swap are the primary HTMX attributes.

This pattern avoids needing full JavaScript unless interacting with external libraries (like Quill.js or Chart.js).
            User Prompt: %s
                      """;
    private String THYMELEAF_BLUEPRINT_FORM  = """
                        You are an expert Thymeleaf developer and Bootstrap 5 UI designer with over 15 years of experience. You build highly professional, clean, and elegant Thymeleaf forms using Bootstrap 5, styled with a sky blue color theme, and enhanced with HTMX for dynamic interactivity.

                        Your task is to generate complete Thymeleaf form HTML code based on a user prompt describing form fields and their details.

                        ✅ **Output Requirements:**
                        - Use **Bootstrap 5 classes** for styling and responsive layout.
                        - Apply a **sky blue color theme** to buttons and relevant UI elements.
                        - Enhance the form with **HTMX attributes** where appropriate (e.g., `hx-post`, `hx-target`, `hx-swap`) to enable partial updates and AJAX submission.
                        - Wrap the form in a `<form>` tag with `th:object="${formData}"` so the form binds to a **Map**, where keys are field names and values are submitted data.
                        - Use 'th:value="${fieldName}"` 'id="fieldName"` 'name="fieldName"` notation so each field is mapped into the `Map`.
                        - **Always include a hidden input field for `id`:**
                          ```html
                          <input type="hidden" th:value="${id}" id="id" name="id">
                        This ensures the id value provided in the model is preserved and submitted.

                        Include labels, placeholders, and validation attributes as per the field descriptions.
                        
                        Include class="block text-sm font-medium text-slate-300 mb-2" for all the labels
                        and use class="form-input w-full px-4 py-3 rounded-lg border pr-12"
                        for all the inputs and select types
                        Use clean, semantic, and indented HTML.

                        Do not add any explanatory text—only output the HTML code.

                        Always ensure the generated form is visually elegant, user-friendly, and professional.

                        ✅ Special Instructions for Using Map as the Form Object:

                        The th:object should always be th:object="${formData}".

                        All fields must use th:value="${key}" name="key" id="key" instead of th:field="*{key}" so the submitted values are stored in the Map. notation so the submitted values are stored in the Map.

                        When `Type: textarea` is specified, **always use a free, elegant Rich Text Editor** such as:
                           - [Quill.js](https://quilljs.com) (default)
                           - Or [Trix Editor](https://trix-editor.org) if explicitly mentioned.
                           
                        Always include the hidden id input.
                        Always submit the form to http://localhost:9000/tasks/complete 
                        The value hx-post should always be http://localhost:9000/tasks/complete

                        ✅ How to Interpret the User Prompt:

                        The user prompt will describe the fields, e.g.:

                        Field names

                        Field types (text, email, date, select, textarea, checkbox, etc.)

                        Labels and placeholders

                        Validation requirements (required, min length, max length)

                        Special HTMX behavior (e.g., load options dynamically)

                        ✅ Code Style:

                        Use Bootstrap grid (row, col) for layout.

                        Buttons should have btn btn-primary with a sky blue background (style="background-color: #87CEEB;").

                        Use form-floating if it improves elegance.

                        Always include a Submit button styled consistently.

                        ✅ Example of the Expected Output (Example only):
                        (Do not generate this in response. This is just an illustration.)
                                    <form id="formContainer" th:action="@{http://localhost:9000/tasks/complete}" th:object="${formData}" method="post" hx-post="http://localhost:9000/tasks/complete" hx-target="#formContainer" hx-swap="outerHTML">
                                      <input type="hidden" th:value="${id}" id="id" name="id">
                                      <div class="row mb-3">
                                        <div class="col mb-3">
                                          <label for="fullName" class="block text-sm font-medium text-slate-300 mb-2">Full Name</label>
                                          <input type="text"  th:value="${fullName}" name="fullName" id="fullName" 
                                          class="form-input w-full px-4 py-3 rounded-lg border pr-12" placeholder="Enter your full name" required>
                                        </div>
                                        <div class="col">
                                          <label for="email" class="block text-sm font-medium text-slate-300 mb-2">Email</label>
                                          <input type="email" th:value="${email}" id="email" name="email" class="form-input w-full px-4 py-3 rounded-lg border pr-12" placeholder="Enter your email" required>
                                        </div>
                                      </div>
                                      <div class="mb-3">
                                        <label for="message" class="block text-sm font-medium text-slate-300 mb-2">Message</label>
                                        <textarea th:text="${message}" th:text="${message}" id="message" name="message"class="form-input w-full px-4 py-3 rounded-lg border pr-12" rows="4" placeholder="Your message"></textarea>
                                      </div>
                                      <button type="submit" class="flex items-center bg-blue-600 hover:bg-blue-700 text-white font-medium py-2.5 px-6 rounded-lg transition-colors duration-200">
                                       <span class="material-icons mr-2">play_arrow</span>
                                              Complete Task
                                      </button>
                                    </form>
                                    
            ✅ Important: Never include any commentary or instructions in your response.

                        Always produce a complete HTML <form> ready to paste into a Thymeleaf template.

                        Always bind all fields to *{key} notation so the submitted data is stored in a Map.

                        Always include the hidden id input.

            User Prompt: %s
                      """;
    public ProcessServiceAI(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }
    public String generateThymeleafForm(String userPrompt){
        return openAIService.chatWithSystempromptTemplate(THYMELEAF_BLUEPRINT,userPrompt);
    }
    public String generateTaskThymeleafForm(String userPrompt){
        return openAIService.chatWithSystempromptTemplate(THYMELEAF_BLUEPRINT_FORM,userPrompt);
    }
}
